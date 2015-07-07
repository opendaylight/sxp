/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.sxp.util.inet.NodeIdConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.PathGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.sxp.database.fields.path.group.prefix.group.BindingBuilder;

public class SxpBindingIdentity {
    public static SxpBindingIdentity create(Binding binding, PrefixGroup prefixGroup, PathGroup pathGroup) {
        return new SxpBindingIdentity(binding, prefixGroup, pathGroup);
    }

    public static String toString(List<SxpBindingIdentity> identities) {
        String result = "";
        for (SxpBindingIdentity bindingDesc : identities) {
            result += bindingDesc.toString() + "\n";
        }
        return result;
    }

    protected Binding binding;

    protected PathGroup pathGroup;

    protected PrefixGroup prefixGroup;

    private SxpBindingIdentity(Binding binding, PrefixGroup prefixGroup, PathGroup pathGroup) {
        super();
        this.binding = new BindingBuilder(binding).build();

        PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder(prefixGroup);
        prefixGroupBuilder.setBinding(new ArrayList<Binding>());
        prefixGroupBuilder.getBinding().add(this.binding);
        this.prefixGroup = prefixGroupBuilder.build();

        PathGroupBuilder pathGroupBuilder = new PathGroupBuilder(pathGroup);
        pathGroupBuilder.setPrefixGroup(new ArrayList<PrefixGroup>());
        pathGroupBuilder.getPrefixGroup().add(this.prefixGroup);
        this.pathGroup = pathGroupBuilder.build();
    }

    public Binding getBinding() {
        return binding;
    }

    public PathGroup getPathGroup() {
        return pathGroup;
    }

    public PrefixGroup getPrefixGroup() {
        return prefixGroup;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        SxpBindingIdentity identity = (SxpBindingIdentity) o;

        if (!binding.equals(identity.binding))
            return false;
        if (!pathGroup.equals(identity.pathGroup))
            return false;
        return prefixGroup.equals(identity.prefixGroup);

    }

    @Override public int hashCode() {
        int result = binding.hashCode();
        result = 31 * result + pathGroup.hashCode();
        result = 31 * result + prefixGroup.hashCode();
        return result;
    }

    @Override
    public String toString() {
        String result = NodeIdConv.toString(pathGroup.getPeerSequence());
        result += " " + prefixGroup.getSgt().getValue();
        result += " " + new String(binding.getIpPrefix().getValue());
        return result;
    }
}
