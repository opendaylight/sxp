package org.opendaylight.sxp.core.it;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import java.io.IOException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.peer.sequence.PeerBuilder;

public class PeerSerializer implements StreamSerializer<Peer> {
    @Override
    public void write(ObjectDataOutput out, Peer object) throws IOException {
        out.writeObject(object.getKey());
        out.writeObject(object.getNodeId());
        out.writeObject(object.getSeq());
    }

    @Override
    public Peer read(ObjectDataInput in) throws IOException {
        return new PeerBuilder().setKey(in.readObject())
                .setNodeId(in.readObject())
                .setSeq(in.readObject())
                .build();
    }

    @Override
    public int getTypeId() {
        return 3;
    }

    @Override
    public void destroy() {

    }
}
