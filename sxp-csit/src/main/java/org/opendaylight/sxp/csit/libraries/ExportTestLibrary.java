/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit.libraries;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.core.service.BindingDispatcher;
import org.opendaylight.sxp.csit.LibraryServer;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.fields.SecurityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.node.identity.fields.TimersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

@RobotKeywords public class ExportTestLibrary extends AbstractLibrary {

    public final static String DESTINATION = "destination";
    private final AtomicLong bindingsRecieved = new AtomicLong(0), bindingsExchange = new AtomicLong(0),
            exportTimeEnd =
                    new AtomicLong(0);
    private long exportTimeBegin, totalOfBindings;

    @RobotKeyword("Get Bindings Exchange Count") @ArgumentNames({}) public synchronized long getExchangeCount() {
        return bindingsExchange.get();
    }

    @RobotKeyword("Get Export Time Total") @ArgumentNames({}) public synchronized double getExportTimeTotal() {
        long time = exportTimeEnd.get();
        return time == 0 ? 0 : (time - exportTimeBegin) / 1000f;
    }

    @RobotKeyword("Get Export Time Current") @ArgumentNames({}) public synchronized double getExportTimeCurrent() {
        double time = getExportTimeTotal();
        return time != 0 ? time : (System.currentTimeMillis() - exportTimeBegin) / 1000f;
    }

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
                        .setTimers(new TimersBuilder().setRetryOpenTime(1).build())
                        .build(), new MasterDatabaseImpl() {

                    @Override public synchronized <T extends SxpBindingFields> List<MasterDatabaseBinding> addBindings(
                            List<T> bindings) {
                        List<MasterDatabaseBinding> result = super.addBindings(bindings);
                        bindingsExchange.addAndGet(result.size());
                        if (bindingsRecieved.addAndGet(result.size()) == totalOfBindings) {
                            exportTimeEnd.set(System.currentTimeMillis());
                        } else {
                            exportTimeEnd.set(0);
                        }
                        return result;
                    }

                    @Override
                    public synchronized <T extends SxpBindingFields> List<MasterDatabaseBinding> deleteBindings(
                            List<T> bindings) {
                        List<MasterDatabaseBinding> result = super.deleteBindings(bindings);
                        bindingsExchange.addAndGet(result.size());
                        if (bindingsRecieved.addAndGet(-result.size()) == totalOfBindings) {
                            exportTimeEnd.set(System.currentTimeMillis());
                        } else {
                            exportTimeEnd.set(0);
                        }
                        return result;
                    }
                }, new SxpDatabaseImpl())).getName();
    }

    @RobotKeyword("Initiate Export") @ArgumentNames({"prefix", "sgt"})
    public synchronized void initiateExport(String prefix, String sgt) {
        List<MasterDatabaseBinding>
                exportBindings =
                Search.expandBinding(new MasterDatabaseBindingBuilder().setPeerSequence(
                        new PeerSequenceBuilder().setPeer(new ArrayList<>()).build())
                        .setIpPrefix(new IpPrefix(Preconditions.checkNotNull(prefix).toCharArray()))
                        .setTimestamp(TimeConv.toDt(System.currentTimeMillis()))
                        .setSecurityGroupTag(new Sgt(Integer.parseInt(Preconditions.checkNotNull(sgt))))
                        .build(), Integer.MAX_VALUE);
        close();
        totalOfBindings = getDestinationNodes() * exportBindings.size();
        LibraryServer.getNodes().stream().filter(node -> node != null && SOURCE.equals(node.getName())).forEach(n -> {
            new BindingDispatcher(n).propagateUpdate(Collections.emptyList(), exportBindings, n.getAllConnections());
        });
        exportTimeBegin = System.currentTimeMillis();
    }

    @RobotKeyword("Initiate Simple Export") @ArgumentNames({"address", "amount"})
    public synchronized void initiateSimpleExport(String address, String amount) {
        close();
        totalOfBindings = getDestinationNodes() * Long.parseLong(amount);
        LibraryServer.getNodes()
                .stream()
                .parallel()
                .filter(node -> node != null && DESTINATION.equals(node.getName()))
                .forEach(
                        node -> addConnection(node, Version.Version4, ConnectionMode.Listener, address, "64999", null));
        exportTimeBegin = System.currentTimeMillis();
    }

    private int getDestinationNodes() {
        return LibraryServer.getNodes()
                .stream()
                .parallel()
                .filter(node -> node != null && DESTINATION.equals(node.getName()))
                .map(n -> 1)
                .reduce(0, Integer::sum);
    }

    @Override public synchronized void close() {
        bindingsRecieved.set(0);
        bindingsExchange.set(0);
        exportTimeEnd.set(0);
        exportTimeBegin = 0;
    }
}
