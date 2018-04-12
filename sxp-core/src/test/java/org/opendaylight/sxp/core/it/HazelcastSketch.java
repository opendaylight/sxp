/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.it;

import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Test;
import org.opendaylight.sxp.test.utils.templates.BindingUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastSketch {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastSketch.class);

    private final MasterDatabaseBinding dummyBinding = BindingUtils.createMasterDBBinding("0.0.0.5/32", 123, "1.1.1.1", "2.2.2.2");

    @Test
    public void hcTest() {
        SerializerConfig mdbSerializerConfig = new SerializerConfig()
                .setImplementation(new MasterDBBindingSerializer())
                .setTypeClass(MasterDatabaseBinding.class);

        SerializerConfig pseqSerializerConfig = new SerializerConfig()
                .setImplementation(new PeerSequenceSerializer())
                .setTypeClass(PeerSequence.class);

        SerializerConfig peerSerializerConfig = new SerializerConfig()
                .setImplementation(new PeerSerializer())
                .setTypeClass(Peer.class);

        Config c = new Config();
        c.getSerializationConfig().addSerializerConfig(mdbSerializerConfig).addSerializerConfig(pseqSerializerConfig)
                .addSerializerConfig(peerSerializerConfig);


        HazelcastInstance instance = Hazelcast.newHazelcastInstance(c);
        IMap<Integer, String> mapCustomers = instance.getMap("customers");
        mapCustomers.put(1, "Joe");
        mapCustomers.put(2, "Ali");
        mapCustomers.put(3, "Avi");

        System.out.println("Customer with key 1: " + mapCustomers.get(1));
        System.out.println("Map Size:" + mapCustomers.size());

        IMap<Integer, SxpBindingFields> mdsalMap = instance.getMap("mdsalMap");
        mdsalMap.put(1, dummyBinding);

        System.out.println("Binding: " + mdsalMap.get(1));

    }


    @Test
    public void testLockingMap() throws InterruptedException {
        Config c = new Config();
        HazelcastInstance i1 = Hazelcast.newHazelcastInstance(c);
        IMap<Integer, String> customers1 = i1.getMap("customers");
        customers1.put(1, "Joe");
        customers1.put(2, "Ali");
        customers1.put(3, "Avi");

        HazelcastInstance i2 = Hazelcast.newHazelcastInstance(c);
        IMap<Integer, String> customers2 = i2.getMap("customers");

        customers1.lock(3);
        try {
            LOG.info("Got a lock");
            new Thread(() -> {
                String s = customers2.get(3);
                LOG.info("Read s {}", s);
                customers2.put(3, "ChangedVal");
                LOG.info("Changed val");
            }).start();
            Thread.sleep(4000);
            LOG.info("Enough sleep");
        } finally {
            customers1.unlock(3);
        }

    }
}
