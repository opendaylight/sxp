/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.util.database;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import java.util.List;
import org.opendaylight.sxp.core.hazelcast.MasterDBBindingSerializer;
import org.opendaylight.sxp.core.hazelcast.PeerSequenceSerializer;
import org.opendaylight.sxp.core.hazelcast.PeerSerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

public class HazelcastBackedMasterDB extends MasterDatabase {

    private final HazelcastInstance hcInstance;

//    private final IMap<BindingType, IMap<IpPrefix, MasterDatabaseBinding>> db; //FIXME: do not nest hazelcast maps, look at stackoverflow


    public HazelcastBackedMasterDB() {
        this(new Config());
    }

    public HazelcastBackedMasterDB(Config hcConfig) {
        hcConfig.getSerializationConfig()
                .addSerializerConfig(MasterDBBindingSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSequenceSerializer.getSerializerConfig())
                .addSerializerConfig(PeerSerializer.getSerializerConfig());
        this.hcInstance = Hazelcast.newHazelcastInstance(hcConfig);
//        this.db = hcInstance.getMap("");//nodeid-domainName-masterDB
    }

    @Override
    public List<MasterDatabaseBinding> getBindings() {
        return null;
    }

    @Override
    public List<MasterDatabaseBinding> getLocalBindings() {
        return null;
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> addLocalBindings(List<T> bindings) {
        return null;
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindingsLocal(List<T> bindings) {
        return null;
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(List<T> bindings) {
        return null;
    }

    @Override
    public <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(List<T> bindings) {
        return null;
    }
}
