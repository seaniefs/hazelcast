/*
 * Copyright (c) 2008-2012, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.semaphore;

import com.hazelcast.config.SemaphoreConfig;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.nio.Address;
import com.hazelcast.partition.MigrationEndpoint;
import com.hazelcast.partition.MigrationType;
import com.hazelcast.spi.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @ali 1/21/13
 */
public class SemaphoreService implements ManagedService, MigrationAwareService, MembershipAwareService, RemoteService {

    public static final String SEMAPHORE_SERVICE_NAME = "hz:impl:semaphoreService";

    public final ConcurrentMap<String, Permit> permitMap = new ConcurrentHashMap<String, Permit>();

    final NodeEngine nodeEngine;

    public SemaphoreService(NodeEngine nodeEngine) {
        this.nodeEngine = nodeEngine;
    }

    public Permit getOrCreatePermit(String name){
        Permit permit = permitMap.get(name);
        if (permit == null){
            SemaphoreConfig config = nodeEngine.getConfig().getSemaphoreConfig(name);
            int partitionId = nodeEngine.getPartitionId(nodeEngine.toData(name));
            permit = new Permit(partitionId, new SemaphoreConfig(config));
            Permit current = permitMap.putIfAbsent(name, permit);
            permit = current == null ? permit : current;
        }
        return permit;
    }

    public void init(NodeEngine nodeEngine, Properties properties) {
        //this.nodeEngine = nodeEngine;
    }

    public void destroy() {
    }

    public void memberAdded(MembershipServiceEvent event) {
    }

    public void memberRemoved(MembershipServiceEvent event) {
        System.out.println("removed: " + event);
        Address caller = event.getMember().getAddress();
        for (Permit permit: permitMap.values()){
            permit.memberRemoved(caller);
        }
    }

    public String getServiceName() {
        return SEMAPHORE_SERVICE_NAME;
    }

    public DistributedObject createDistributedObject(Object objectId) {
        return new SemaphoreProxy((String)objectId, this, nodeEngine);
    }

    public DistributedObject createDistributedObjectForClient(Object objectId) {
        return createDistributedObject(objectId);
    }

    public void destroyDistributedObject(Object objectId) {
    }

    public void beforeMigration(MigrationServiceEvent migrationServiceEvent) {
    }

    public Operation prepareMigrationOperation(MigrationServiceEvent event) {
        if (event.getPartitionId() < 0 || event.getPartitionId() >= nodeEngine.getPartitionCount()) {
            return null; // is it possible
        }
        Map<String, Permit> migrationData = new HashMap<String, Permit>();
        for (Map.Entry<String, Permit> entry: permitMap.entrySet()){
            String name = entry.getKey();
            Permit permit = entry.getValue();
            if (permit.getPartitionId() == event.getPartitionId() && permit.getConfig().getTotalBackupCount() >= event.getReplicaIndex()){
                migrationData.put(name, permit);
            }
        }
        if (migrationData.isEmpty()){
            return null;
        }
        return new SemaphoreMigrationOperation();
    }

    public void insertMigrationData(Map<String, Permit> migrationData){
        permitMap.putAll(migrationData);
    }

    public void commitMigration(MigrationServiceEvent event) {
        if (event.getMigrationEndpoint() == MigrationEndpoint.SOURCE){
            if (event.getMigrationType() == MigrationType.MOVE || event.getMigrationType() == MigrationType.MOVE_COPY_BACK){
                clearMigrationData(event.getPartitionId(), event.getCopyBackReplicaIndex());
            }
        }
    }

    private void clearMigrationData(int partitionId, int copyBack){
        Iterator<Map.Entry<String, Permit>> iter = permitMap.entrySet().iterator();
        while (iter.hasNext()){
            Permit permit = iter.next().getValue();
            if (permit.getPartitionId() == partitionId && (copyBack == -1 || permit.getConfig().getTotalBackupCount() < copyBack)){
                iter.remove();
            }
        }
    }

    public void rollbackMigration(MigrationServiceEvent event) {
        if (event.getMigrationEndpoint() == MigrationEndpoint.DESTINATION) {
            clearMigrationData(event.getPartitionId(), -1);
        }
    }

    public int getMaxBackupCount() {
        int max = 0;
        for (String name: permitMap.keySet()){
            SemaphoreConfig config = nodeEngine.getConfig().getSemaphoreConfig(name);
            max = Math.max(max,  config.getTotalBackupCount());
        }
        return max;
    }
}