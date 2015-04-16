/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.util.database;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.opendaylight.sxp.util.inet.IpPrefixConv;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.DatabaseAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.Source;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.PrefixGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev141002.master.database.fields.source.prefix.group.BindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev141002.sxp.databases.fields.MasterDatabase;

public class MasterBindingIdentity {

    private boolean deleteReplace;

    public static MasterBindingIdentity create(Binding binding, PrefixGroup prefixGroup, Source source) {
        return new MasterBindingIdentity(binding, prefixGroup, source);
    }

    public static List<MasterBindingIdentity> create(MasterDatabase database, boolean onlyChanged) {
        List<MasterBindingIdentity> identities = new ArrayList<>();
        Map<String, Binding> deleted = Maps.newHashMap();
        Multimap<String, MasterBindingIdentity> deletedReplacementCandidates = HashMultimap.create();

        if (database.getSource() != null) {
            for (Source source : database.getSource()) {
                if (source.getPrefixGroup() != null) {
                    for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                        if (prefixGroup.getBinding() != null) {
                            for (Binding binding : prefixGroup.getBinding()) {
                                Boolean changed = binding.isChanged();
                                if (changed == null) {
                                    changed = true;
                                }

                                if(binding.getAction() == DatabaseAction.Delete) {
                                    // Fixme string as key
                                    deleted.put(new String(binding.getKey().getIpPrefix().getValue()), binding);
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

        if (database.getSource() != null) {
            for (Source source : database.getSource()) {
                if (source.getPrefixGroup() != null) {
                    for (PrefixGroup prefixGroup : source.getPrefixGroup()) {
                        if (prefixGroup.getBinding() != null) {
                            for (Binding binding : prefixGroup.getBinding()) {
                                if(binding.getAction() == DatabaseAction.Delete) {
                                    continue;
                                }
                                // Add delete replacements
                                final String key = new String(binding.getKey().getIpPrefix().getValue());
                                if(deleted.containsKey(key)) {
                                    deletedReplacementCandidates.put(key, new MasterBindingIdentity(binding, prefixGroup, source, true));
                                }
                            }
                        }
                    }
                }
            }
        }

        ArrayList<MasterBindingIdentity> candidates;
        for (String key : deletedReplacementCandidates.keySet()) {
            candidates = Lists.newArrayList(deletedReplacementCandidates.get(key));
            Collections.sort(candidates, new Comparator<MasterBindingIdentity>() {
                @Override
                public int compare(final MasterBindingIdentity b1, final MasterBindingIdentity b2) {
                    final Binding o1 = b1.getBinding();
                    final Binding o2 = b2.getBinding();
                    final int peerSequenceComparison = -1 * (o1.getPeerSequence().getPeer().size() - o2.getPeerSequence().getPeer().size());
                    if(peerSequenceComparison == 0) {
                        try {
                            return Long.valueOf(TimeConv.toLong(o1.getTimestamp())).compareTo(TimeConv.toLong(o2.getTimestamp()));
                        // FIXME base exception type thrown
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Unable to parse datetime to long", e);
                        }
                    }
                    return peerSequenceComparison;
                }
            });

            // Adding the most fittest binding replacement for deleted IP
            identities.add(candidates.get(0));
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