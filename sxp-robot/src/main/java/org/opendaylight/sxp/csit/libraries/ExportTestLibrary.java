/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit.libraries;

import com.google.common.base.Preconditions;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.csit.LibraryServer;
import org.opendaylight.sxp.csit.RobotLibraryServer;
import org.opendaylight.sxp.util.database.MasterDatabaseImpl;
import org.opendaylight.sxp.util.database.SxpDatabaseImpl;
import org.opendaylight.sxp.util.inet.Search;
import org.opendaylight.sxp.util.time.TimeConv;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.SxpBindingFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.peer.sequence.fields.PeerSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.sxp.database.fields.binding.database.binding.sources.binding.source.sxp.database.bindings.SxpDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Robot library used for Bindings export/forwarding measuring
 */
@RobotKeywords public class ExportTestLibrary extends AbstractLibrary {

    public final static String DESTINATION = "destination";
    private final AtomicLong bindingsReceived = new AtomicLong(0), exportTimeEnd = new AtomicLong(0);
    private long exportTimeBegin, totalOfBindings;

    /**
     * @param libraryServer Server where Library will be added
     */
    public ExportTestLibrary(RobotLibraryServer libraryServer) {
        super(libraryServer);
    }

    /**
     * @return Amount of bindings that were processed
     */
    @RobotKeyword("Get Bindings Exchange Count") @ArgumentNames({}) public synchronized long getExchangeCount() {
        return bindingsReceived.get();
    }

    /**
     * @return Time elapsed while exporting all bindings or 0 if exports is still in progress
     */
    @RobotKeyword("Get Export Time") @ArgumentNames({}) public synchronized double getExportTime() {
        long time = exportTimeEnd.get();
        return time == 0 ? 1 : (time - exportTimeBegin) / 1000f;
    }

    /**
     * @return If all bindings were exported
     */
    @RobotKeyword("All Exported") @ArgumentNames({}) public synchronized boolean getAllExported() {
        return totalOfBindings <= bindingsReceived.get();
    }

    /**
     * @param amount Amount of bindings that will be exported between nodes
     */
    @RobotKeyword("Set Export Amount") @ArgumentNames({"amount"}) public synchronized void setExportAmount(
            String amount) {
        totalOfBindings = (long) Double.parseDouble(amount);
    }

    /**
     * Adds Node that is used as Destination  of bindings export
     *
     * @param nodeId   Id of SxpNode
     * @param version  Default version used
     * @param port     Port of SxpNode
     * @param password Password used by TCP-MD5
     */
    @RobotKeyword("Add Destination Node") @ArgumentNames({"node_id", "version", "port", "password"})
    public synchronized void addDestinationNode(String nodeId, String version, String port, String password) {
        LibraryServer.putNode(SxpNode.createInstance(new NodeId(nodeId),
                new SxpNodeIdentityBuilder().setSourceIp(new IpAddress(nodeId.toCharArray()))
                        .setCapabilities(Configuration.getCapabilities(getVersion(version)))
                        .setEnabled(true)
                        .setName(DESTINATION)
                        .setVersion(getVersion(version))
                        .setTcpPort(new PortNumber(Integer.parseInt(port)))
                        .setSecurity(new SecurityBuilder().setPassword(
                                password == null || password.isEmpty() ? null : password).build())
                        .setTimers(new TimersBuilder().setRetryOpenTime(5).build())
                        .build(), new MasterDatabaseImpl(), new SxpDatabaseImpl() {

                    @Override public synchronized <T extends SxpBindingFields> List<SxpDatabaseBinding> deleteBindings(
                            NodeId nodeId, List<T> bindings) {
                        if (bindingsReceived.addAndGet(-bindings.size()) == totalOfBindings) {
                            exportTimeEnd.set(System.currentTimeMillis());
                        }
                        return Collections.emptyList();
                    }

                    @Override
                    public synchronized <T extends SxpBindingFields> List<SxpDatabaseBinding> addBinding(NodeId nodeId,
                            List<T> bindings) {
                        if (bindingsReceived.addAndGet(bindings.size()) == totalOfBindings) {
                            exportTimeEnd.set(System.currentTimeMillis());
                        }
                        return Collections.emptyList();
                    }
                }));
    }

    /**
     * Expand and exports bindings fromSource nodes to Destination nodes and start measuring forwarding speed
     *
     * @param prefix IpPrefix of binding that will be expanded and send
     * @param sgt    Sgt of binding
     */
    @RobotKeyword("Initiate Export") @ArgumentNames({"prefix", "sgt"}) public synchronized void initiateExport(
            String prefix, String sgt) {
        final List<MasterDatabaseBinding>
                exportBindings =
                Search.expandBinding(new MasterDatabaseBindingBuilder().setPeerSequence(
                        new PeerSequenceBuilder().setPeer(new ArrayList<>()).build())
                        .setIpPrefix(new IpPrefix(Preconditions.checkNotNull(prefix).toCharArray()))
                        .setTimestamp(TimeConv.toDt(System.currentTimeMillis()))
                        .setSecurityGroupTag(new Sgt(Integer.parseInt(Preconditions.checkNotNull(sgt))))
                        .build(), Integer.MAX_VALUE);
        totalOfBindings =
                totalOfBindings == 0 ?
                        getDestinationNodes() * exportBindings.size() : getDestinationNodes() * totalOfBindings;
        LibraryServer.getNodes().stream().filter(node -> node != null && SOURCE.equals(node.getName())).forEach(n -> {
            new BindingDispatcher(n).propagateUpdate(Collections.emptyList(), exportBindings, n.getAllConnections());
        });
        exportTimeBegin = System.currentTimeMillis();
    }

    /**
     * Connects Destination Nodes to tested nodes and start measuring export speed
     *
     * @param address Address of tested Node
     */
    @RobotKeyword("Initiate Simple Export") @ArgumentNames({"address"}) public synchronized void initiateSimpleExport(
            String address) {
        totalOfBindings = getDestinationNodes() * totalOfBindings;
        LibraryServer.getNodes()
                .stream()
                .parallel()
                .filter(node -> node != null && DESTINATION.equals(node.getName()))
                .forEach(
                        node -> addConnection(node, Version.Version4, ConnectionMode.Listener, address, "64999", null));
        exportTimeBegin = System.currentTimeMillis();
    }

    /**
     * @return Sum of SxpNodes marked as Destination Nodes
     */
    private int getDestinationNodes() {
        return LibraryServer.getNodes()
                .stream()
                .parallel()
                .filter(node -> node != null && DESTINATION.equals(node.getName()))
                .map(n -> 1)
                .reduce(0, Integer::sum);
    }

    @Override public synchronized void close() {
        bindingsReceived.set(0);
        exportTimeEnd.set(0);
        exportTimeBegin = 0;
        totalOfBindings = 0;
    }
}
