package org.opendaylight.sxp.core.it;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import java.io.IOException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequence;

public class MasterDBBindingSerializer implements StreamSerializer<MasterDatabaseBinding> {

    @Override
    public void write(ObjectDataOutput out, MasterDatabaseBinding object) throws IOException {
        out.writeObject(object.getIpPrefix());
        out.writeObject(object.getKey());
        out.writeObject(object.getPeerSequence());
        out.writeObject(object.getSecurityGroupTag());
        out.writeObject(object.getTimestamp());
    }

    @Override
    public MasterDatabaseBinding read(ObjectDataInput in) throws IOException {
        IpPrefix ipPrefix = in.readObject();
        MasterDatabaseBindingKey key = in.readObject();
        PeerSequence ps = in.readObject();
        Sgt binding = in.readObject();
        DateAndTime date = in.readObject();
        return new MasterDatabaseBindingBuilder().setIpPrefix(ipPrefix)
                .setKey(key)
                .setPeerSequence(ps)
                .setSecurityGroupTag(binding)
                .setTimestamp(date)
                .build();
    }

    @Override
    public int getTypeId() {
        return 1;
    }

    @Override
    public void destroy() {

    }
}
