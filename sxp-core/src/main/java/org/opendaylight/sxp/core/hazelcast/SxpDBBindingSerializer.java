/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.hazelcast;

import com.hazelcast.config.SerializerConfig;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import java.io.IOException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.config.rev180611.OriginType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBindingKey;

public class SxpDBBindingSerializer implements StreamSerializer<SxpDatabaseBinding> {


    @Override
    public void write(ObjectDataOutput out, SxpDatabaseBinding object) throws IOException {
        out.writeObject(object.getIpPrefix());
        out.writeObject(object.getOrigin());
        out.writeObject(object.key());
        out.writeObject(object.getPeerSequence());
        out.writeObject(object.getSecurityGroupTag());
        out.writeObject(object.getTimestamp());
    }

    @Override
    public SxpDatabaseBinding read(ObjectDataInput in) throws IOException {
        IpPrefix ipPrefix = in.readObject();
        OriginType originType = in.readObject();
        SxpDatabaseBindingKey key = in.readObject();
        PeerSequence ps = in.readObject();
        Sgt binding = in.readObject();
        DateAndTime date = in.readObject();
        return new SxpDatabaseBindingBuilder()
                .withKey(key)
                .setIpPrefix(ipPrefix)
                .setOrigin(originType)
                .setPeerSequence(ps)
                .setSecurityGroupTag(binding)
                .setTimestamp(date)
                .build();
    }

    @Override
    public int getTypeId() {
        return 4;//TODO: Autogenerate uids
    }

    @Override
    public void destroy() {
    }

    public static SerializerConfig getSerializerConfig() {
        return new SerializerConfig()
                .setImplementation(new SxpDBBindingSerializer())
                .setTypeClass(SxpDatabaseBinding.class);
    }
}
