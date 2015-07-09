/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;

import java.util.ArrayList;
import java.util.List;

public class MasterBindingIdentity {

    private boolean deleteReplace;

    public static MasterBindingIdentity create(Binding binding, PrefixGroup prefixGroup, Source source) {
        return new MasterBindingIdentity(binding, prefixGroup, source);
    }

    public static List<MasterBindingIdentity> create(MasterDatabase database, boolean onlyChanged) {
        List<MasterBindingIdentity> identities = new ArrayList<>();

        if (database != null && database.getSource() != null) {
            for (Source source : database.getSource()) {
                if (source.getPrefixGroup() != null) {
                    for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                        if (prefixGroup.getBinding() != null) {
                            for (Binding binding : prefixGroup.getBinding()) {
                                Boolean changed = binding.isChanged();
                                if (changed == null) {
                                    changed = true;
                                }
                                if (!onlyChanged || changed) {
                                    identities.add(new MasterBindingIdentity(binding, prefixGroup, source));
                                }
                            }
                        }
                    }
                }
            }
        }
        return identities;
    }

    public static String toString(List<MasterBindingIdentity> identities) {
        String result = "";
        for (MasterBindingIdentity bindingDesc : identities) {
            result += bindingDesc.toString() + "\n";
        }
        return result;
    }

    protected Binding binding;

    protected PrefixGroup prefixGroup;

    protected Source source;

    private MasterBindingIdentity(Binding binding, PrefixGroup prefixGroup, Source source) {
        this(binding, prefixGroup, source, false);
    }

    private MasterBindingIdentity(Binding binding, PrefixGroup prefixGroup, Source source, boolean deleteReplace) {
        super();
        this.binding = new BindingBuilder(binding).build();

        PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder(prefixGroup);
        prefixGroupBuilder.setBinding(new ArrayList<Binding>());
        prefixGroupBuilder.getBinding().add(this.binding);
        this.prefixGroup = prefixGroupBuilder.build();

        SourceBuilder sourceBuilder = new SourceBuilder(source);
        sourceBuilder.setPrefixGroup(new ArrayList<PrefixGroup>());
        sourceBuilder.getPrefixGroup().add(this.prefixGroup);
        this.source = sourceBuilder.build();
        this.deleteReplace = deleteReplace;
    }

    /**
     * @return true if current identity is a fallback binding for a deleted binding with same ip
     */
    public boolean isDeleteReplace() {
        return deleteReplace;
    }

    @Override public int hashCode() {
        int result = binding.hashCode();
        result = 31 * result + prefixGroup.hashCode();
        result = 31 * result + source.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MasterBindingIdentity)) {
            return false;
        }
        MasterBindingIdentity bindingIdentity = (MasterBindingIdentity) object;

        return this.source.getBindingSource().equals(bindingIdentity.source.getBindingSource())
                && this.prefixGroup.getSgt().getValue().equals(bindingIdentity.prefixGroup.getSgt().getValue())
                && IpPrefixConv.equalTo(this.binding.getIpPrefix(), bindingIdentity.binding.getIpPrefix());
    }

    public Binding getBinding() {
        return binding;
    }

    public PrefixGroup getPrefixGroup() {
        return prefixGroup;
    }

    public Source getSource() {
        return source;
    }

    @Override
    public String toString() {
        String result = source.getBindingSource().toString();
        result += " " + prefixGroup.getSgt().getValue();
        result += " " + new String(binding.getIpPrefix().getValue());
        return result;
    }
}
