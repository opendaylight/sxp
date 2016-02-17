/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import com.google.common.base.Preconditions;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * MasterBindingIdentity class represent entity stored in MasterDatabase
 */
public class MasterBindingIdentity {

    private boolean deleteReplace;

    /**
     * Creates MasterBindingIdentity from specified values
     *
     * @param binding     Binding to be used
     * @param prefixGroup PrefixGroup to be used
     * @param source      Source to be used
     * @return Newly created MasterBindingIdentity
     */
    public static MasterBindingIdentity create(Binding binding, PrefixGroup prefixGroup, Source source) {
        return new MasterBindingIdentity(binding, prefixGroup, source);
    }

    /**
     * Creates MasterBindingIdentities from MasterDatabase
     *
     * @param database    MasterDatabase where to look for data
     * @param onlyChanged If add only Bindings with Flag Changed
     * @return List of newly created Bindings
     */
    public static List<MasterBindingIdentity> create(MasterDatabase database, boolean onlyChanged) {
        List<MasterBindingIdentity> identities = new ArrayList<>();

        if (database != null && database.getSource() != null) {
            for (Source source : database.getSource()) {
                if (source.getPrefixGroup() != null) {
                    for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                        if (prefixGroup.getBinding() != null) {
                            for (Binding binding : prefixGroup.getBinding()) {
                                if (!onlyChanged || binding.isChanged()) {
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

    /**
     * Create String representation of MasterBindingIdentities
     *
     * @param identities List of MasterBindingIdentity to be used
     * @return String representations of specified data
     */
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

    /**
     * Constructor that creates MasterBindingIdentity using provided data
     * and set Flag deleteReplace to false
     *
     * @param binding       Binding to be used
     * @param prefixGroup   PrefixGroup to be used
     * @param source        Source to be used
     */
    private MasterBindingIdentity(Binding binding, PrefixGroup prefixGroup, Source source) {
        this(binding, prefixGroup, source, false);
    }

    /**
     * Default constructor that creates MasterBindingIdentity using provided data
     *
     * @param binding       Binding to be used
     * @param prefixGroup   PrefixGroup to be used
     * @param source        Source to be used
     * @param deleteReplace If MasterBindingIdentity is replacement,
     *                      for MasterBindingIdentity with Flag Delete
     */
    private MasterBindingIdentity(Binding binding, PrefixGroup prefixGroup, Source source, boolean deleteReplace) {
        if (Preconditions.checkNotNull(binding).getIpPrefix().getIpv6Prefix() != null) {
            this.binding =
                    new BindingBuilder(binding).setKey(new BindingKey(
                            new IpPrefix(binding.getIpPrefix().getIpv6Prefix().getValue().toLowerCase().toCharArray())))
                            .build();
        } else {
            this.binding = new BindingBuilder(binding).build();
        }
        PrefixGroupBuilder prefixGroupBuilder = new PrefixGroupBuilder(Preconditions.checkNotNull(prefixGroup));
        prefixGroupBuilder.setBinding(new ArrayList<Binding>());
        prefixGroupBuilder.getBinding().add(this.binding);
        this.prefixGroup = prefixGroupBuilder.build();

        SourceBuilder sourceBuilder = new SourceBuilder(Preconditions.checkNotNull(source));
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

    /**
     * @return Gets Binding
     */
    public Binding getBinding() {
        return binding;
    }

    /**
     * @return Gets PrefixGroup
     */
    public PrefixGroup getPrefixGroup() {
        return prefixGroup;
    }

    /**
     * @return Gets Source
     */
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
