/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.controller.listeners.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.FilterEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.MaskRangeOperator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.PrefixListMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.WildcardMask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.entry.AclMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.entry.AclMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.acl.match.fields.MaskBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.FilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.AclFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PeerSequenceFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PeerSequenceFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PrefixListFilterEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.PrefixListFilterEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.acl.filter.entries.AclEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.peer.sequence.filter.entries.PeerSequenceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.peer.sequence.filter.entries.PeerSequenceEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.prefix.list.filter.entries.PrefixListEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.filter.entries.fields.filter.entries.prefix.list.filter.entries.PrefixListEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.entry.PrefixListMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.SgtMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtMatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.sgt.match.fields.sgt.match.SgtRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpConnectionFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.opendaylight.yangtools.yang.binding.DataObject;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkDifference;
import static org.opendaylight.sxp.controller.listeners.spi.Listener.Differences.checkFilterEntries;

public class DifferencesTest {

    private <T extends DataObject> DataTreeModification<T> getModification(T before, T after) {
        DataTreeModification<T> tree = mock(DataTreeModification.class);
        DataObjectModification<T> modification = mock(DataObjectModification.class);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataAfter()).thenReturn(after);
        when(tree.getRootNode()).thenReturn(modification);
        return tree;
    }

    private Connection getChange(String address, int port, Version version, ConnectionMode mode) {
        return new ConnectionBuilder().setVersion(version)
                .setMode(mode)
                .setPeerAddress(new IpAddress(address.toCharArray()))
                .setTcpPort(new PortNumber(port))
                .build();
    }

    private List<Connection> getChanges(Connection... connection) {
        return Arrays.asList(connection);
    }

    @Test public void testCheckDifference() throws Exception {
        assertTrue(checkDifference(
                getModification(getChange("127.0.0.1", 64999, Version.Version4, ConnectionMode.Speaker),
                        getChange("127.0.0.2", 64999, Version.Version4, ConnectionMode.Speaker)),
                SxpConnectionFields::getPeerAddress));

        assertTrue(checkDifference(
                getModification(getChange("127.0.0.1", 64990, Version.Version4, ConnectionMode.Speaker),
                        getChange("127.0.0.1", 64999, Version.Version4, ConnectionMode.Speaker)),
                SxpConnectionFields::getTcpPort));

        assertFalse(checkDifference(
                getModification(getChange("127.0.0.5", 64999, Version.Version4, ConnectionMode.Speaker),
                        getChange("127.0.0.1", 64999, Version.Version4, ConnectionMode.Speaker)),
                SxpConnectionFields::getVersion));

        assertFalse(checkDifference(
                getModification(getChange("127.0.0.1", 64999, Version.Version2, ConnectionMode.Speaker),
                        getChange("127.0.0.1", 64999, Version.Version4, ConnectionMode.Speaker)),
                SxpConnectionFields::getPeerAddress));

        Connection change1 = getChange("127.0.0.1", 64999, Version.Version2, ConnectionMode.Speaker),
                change2 = getChange("127.0.0.2", 64999, Version.Version2, ConnectionMode.Speaker),
                change3 = getChange("127.0.0.1", 64998, Version.Version2, ConnectionMode.Speaker);

        assertFalse(checkDifference(new ArrayList<>(), new ArrayList<>()));
        assertFalse(checkDifference(getChanges(change1, change2), getChanges(change1, change2)));
        assertFalse(checkDifference(getChanges(change2, change1), getChanges(change1, change2)));

        assertTrue(checkDifference(getChanges(change1, change2, change3), getChanges(change1, change2)));
        assertTrue(checkDifference(getChanges(change1, change2), getChanges(change1, change2, change3)));
        assertTrue(checkDifference(getChanges(change1, change2), new ArrayList<>()));
        assertTrue(checkDifference(new ArrayList<>(), getChanges(change1, change2)));
        assertTrue(checkDifference(getChanges(change1, change2), getChanges(change1)));
        assertTrue(checkDifference(getChanges(change1, change2), getChanges(change1, change3)));
        assertTrue(checkDifference(getChanges(change3, change2), getChanges(change1, change3)));
    }

    @Test public void testCheckFilterEntry() throws Exception {
        FilterEntries entries1, entries2;

        entries1 = getPeerSequenceEntries(getPeerSequenceEntry(1, FilterEntryType.Permit, 10, MaskRangeOperator.Eq));
        entries2 =
                getPrefixListEntries(getPrefixListEntry(1, FilterEntryType.Deny,
                        getPrefixListMatch("5.5.5.5/32", (short) 24, MaskRangeOperator.Eq), getSgtMatch(1, 5)));
        assertTrue(checkFilterEntries(entries1, entries2));
        assertFalse(checkFilterEntries(entries1, entries1));
        assertFalse(checkFilterEntries(entries2, entries2));

        entries1 =
                getPrefixListEntries(getPrefixListEntry(1, FilterEntryType.Deny,
                        getPrefixListMatch("5.5.5.5/32", (short) 24, MaskRangeOperator.Eq), getSgtMatch(5, 25)));
        assertTrue(checkFilterEntries(entries1, entries2));

        entries1 =
                getPrefixListEntries(getPrefixListEntry(1, FilterEntryType.Deny,
                        getPrefixListMatch("5.5.5.5/32", (short) 25, MaskRangeOperator.Eq), getSgtMatch(1, 5)));
        assertTrue(checkFilterEntries(entries1, entries2));

        entries1 =
                getPrefixListEntries(getPrefixListEntry(1, FilterEntryType.Deny,
                        getPrefixListMatch("5.25.5.5/32", (short) 24, MaskRangeOperator.Eq), getSgtMatch(1, 5)));
        assertTrue(checkFilterEntries(entries1, entries2));

        entries2 =
                getPrefixListEntries(getPrefixListEntry(1, FilterEntryType.Permit,
                        getPrefixListMatch("5.5.5.5/32", (short) 24, MaskRangeOperator.Eq), getSgtMatch(1, 5)));
        assertTrue(checkFilterEntries(entries1, entries2));

        entries2 =
                getPrefixListEntries(getPrefixListEntry(10, FilterEntryType.Deny,
                        getPrefixListMatch("5.5.5.5/32", (short) 24, MaskRangeOperator.Eq), getSgtMatch(1, 5)));
        assertTrue(checkFilterEntries(entries1, entries2));

        entries1 = getPeerSequenceEntries(getPeerSequenceEntry(1, FilterEntryType.Permit, 10, MaskRangeOperator.Eq));
        entries2 = getPeerSequenceEntries(getPeerSequenceEntry(1, FilterEntryType.Permit, 10, MaskRangeOperator.Ge));
        assertTrue(checkFilterEntries(entries1, entries2));

        entries2 = getPeerSequenceEntries(getPeerSequenceEntry(1, FilterEntryType.Permit, 100, MaskRangeOperator.Eq));
        assertTrue(checkFilterEntries(entries1, entries2));

        entries2 = getPeerSequenceEntries(getPeerSequenceEntry(1, FilterEntryType.Deny, 10, MaskRangeOperator.Eq));
        assertTrue(checkFilterEntries(entries1, entries2));

        entries2 = getPeerSequenceEntries(getPeerSequenceEntry(10, FilterEntryType.Permit, 10, MaskRangeOperator.Eq));
        assertTrue(checkFilterEntries(entries1, entries2));

        entries1 =
                getAclEntries(getAclEntry(1, FilterEntryType.Deny,
                        getAclMatch("1.1.1.1", "255.255.255.0", "1.1.1.1", "255.255.255.0"),
                        getSgtMatch(getSgt(5), getSgt(8), getSgt(2))));
        entries2 =
                getAclEntries(getAclEntry(1, FilterEntryType.Deny,
                        getAclMatch("1.1.1.1", "255.255.255.0", "1.1.1.1", "255.255.255.0"),
                        getSgtMatch(getSgt(5), getSgt(8), getSgt(20))));
        assertTrue(checkFilterEntries(entries1, entries2));
        entries2 =
                getAclEntries(getAclEntry(1, FilterEntryType.Deny,
                        getAclMatch("1.1.1.1", "255.255.255.0", "1.1.1.1", "255.255.255.0"),
                        getSgtMatch(getSgt(5), getSgt(80), getSgt(2))));
        assertTrue(checkFilterEntries(entries1, entries2));
        entries2 =
                getAclEntries(getAclEntry(1, FilterEntryType.Deny,
                        getAclMatch("1.1.1.1", "25.255.255.0", "1.1.1.1", "255.255.255.0"),
                        getSgtMatch(getSgt(5), getSgt(8), getSgt(2))));
        assertTrue(checkFilterEntries(entries1, entries2));
        entries2 =
                getAclEntries(getAclEntry(1, FilterEntryType.Permit,
                        getAclMatch("1.1.1.1", "255.255.255.0", "1.1.1.1", "255.255.255.0"),
                        getSgtMatch(getSgt(5), getSgt(8), getSgt(2))));
        assertTrue(checkFilterEntries(entries1, entries2));
        entries2 =
                getAclEntries(getAclEntry(11, FilterEntryType.Deny,
                        getAclMatch("1.1.1.1", "255.255.255.0", "1.1.1.1", "255.255.255.0"),
                        getSgtMatch(getSgt(5), getSgt(8), getSgt(2))));
        assertTrue(checkFilterEntries(entries1, entries2));
    }

    private SgtMatch getSgtMatch(Sgt... sgts) {
        return new SgtMatchesBuilder().setMatches(Arrays.asList(sgts)).build();
    }

    private SgtMatch getSgtMatch(int start, int end) {
        return new SgtRangeBuilder().setSgtStart(getSgt(start)).setSgtEnd(getSgt(end)).build();
    }

    private Sgt getSgt(int sgt) {
        return new Sgt(sgt);
    }

    private AclFilterEntries getAclEntries(AclEntry... entries) {
        AclFilterEntriesBuilder builder = new AclFilterEntriesBuilder().setAclEntry(new ArrayList<>());
        Collections.addAll(builder.getAclEntry(), entries);
        return builder.build();
    }

    private AclEntry getAclEntry(int seq, FilterEntryType type, AclMatch aclMatch, SgtMatch sgtMatch) {
        return new AclEntryBuilder().setEntrySeq(seq)
                .setEntryType(type)
                .setAclMatch(aclMatch)
                .setSgtMatch(sgtMatch)
                .build();
    }

    private AclMatch getAclMatch(String ip, String mask, String subIp, String subMask) {
        return new AclMatchBuilder().setIpAddress(new IpAddress(ip.toCharArray()))
                .setMask(new MaskBuilder().setAddressMask(new IpAddress(subIp.toCharArray()))
                        .setWildcardMask(new WildcardMask(subMask.toCharArray()))
                        .build())
                .setWildcardMask(new WildcardMask(mask.toCharArray()))
                .build();
    }

    private PrefixListFilterEntries getPrefixListEntries(PrefixListEntry... entries) {
        PrefixListFilterEntriesBuilder
                builder =
                new PrefixListFilterEntriesBuilder().setPrefixListEntry(new ArrayList<>());
        Collections.addAll(builder.getPrefixListEntry(), entries);
        return builder.build();
    }

    private PrefixListEntry getPrefixListEntry(int seq, FilterEntryType type, PrefixListMatch prefixListMatch,
            SgtMatch sgtMatch) {
        return new PrefixListEntryBuilder().setEntrySeq(seq)
                .setEntryType(type)
                .setPrefixListMatch(prefixListMatch)
                .setSgtMatch(sgtMatch)
                .build();
    }

    private PrefixListMatch getPrefixListMatch(String prefix, short mask, MaskRangeOperator rangeOperator) {
        return new PrefixListMatchBuilder().setMask(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.filter.rev150911.prefix.list.match.fields.MaskBuilder()
                        .setMaskRange(rangeOperator)
                        .setMaskValue(new PrefixListMask(mask))
                        .build()).setIpPrefix(new IpPrefix(prefix.toCharArray())).build();
    }

    private PeerSequenceFilterEntries getPeerSequenceEntries(PeerSequenceEntry... entries) {
        PeerSequenceFilterEntriesBuilder
                builder =
                new PeerSequenceFilterEntriesBuilder().setPeerSequenceEntry(new ArrayList<>());
        Collections.addAll(builder.getPeerSequenceEntry(), entries);
        return builder.build();
    }

    private PeerSequenceEntry getPeerSequenceEntry(int seq, FilterEntryType type, int seqLen,
            MaskRangeOperator rangeOperator) {
        return new PeerSequenceEntryBuilder().setEntrySeq(seq)
                .setEntryType(type)
                .setPeerSequenceLength(seqLen)
                .setPeerSequenceRange(rangeOperator)
                .build();
    }
}
