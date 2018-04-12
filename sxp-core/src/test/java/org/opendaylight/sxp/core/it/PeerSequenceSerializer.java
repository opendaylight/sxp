/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sxp.core.it;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import java.io.IOException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;

public class PeerSequenceSerializer implements StreamSerializer<PeerSequence> {
    @Override
    public void write(ObjectDataOutput out, PeerSequence object) throws IOException {
        out.writeObject(object.getPeer());
    }

    @Override
    public PeerSequence read(ObjectDataInput in) throws IOException {
        return new PeerSequenceBuilder().setPeer(in.readObject()).build();
    }

    @Override
    public int getTypeId() {
        return 2;
    }

    @Override
    public void destroy() {

    }
}
