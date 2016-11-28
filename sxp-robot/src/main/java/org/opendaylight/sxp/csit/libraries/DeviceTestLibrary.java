/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.csit.libraries;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import org.opendaylight.sxp.core.Configuration;
import org.opendaylight.sxp.core.SxpNode;
import org.opendaylight.sxp.csit.LibraryServer;
import org.opendaylight.sxp.csit.RobotLibraryServer;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.Version;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

/**
 * Robot library used for emulation of resource low SXP device
 */
@RobotKeywords public class DeviceTestLibrary extends AbstractLibrary {

    private static final ObjectMapper pojoBindingsSerializer = new ObjectMapper();

    /**
     * Robot library constructor
     */
    public DeviceTestLibrary() {
        super();
    }

    /**
     * @param libraryServer Server where Library will be added
     */
    public DeviceTestLibrary(RobotLibraryServer libraryServer) {
        super(libraryServer);
        connectionTimers.setReconciliationTime(0);
        connectionTimers.setDeleteHoldDownTime(0);
        pojoBindingsSerializer.registerModule(
                new SimpleModule().addSerializer(SxpBindingFields.class, new JsonSerializer<SxpBindingFields>() {

                    @Override
                    public void serialize(SxpBindingFields value, JsonGenerator jgen, SerializerProvider provider)
                            throws IOException {
                        jgen.writeStartObject();
                        jgen.writeNumberField("sgt", value.getSecurityGroupTag().getValue());
                        jgen.writeArrayFieldStart("ip-prefix");
                        jgen.writeString(new String(value.getIpPrefix().getValue()));
                        jgen.writeEndArray();
                        jgen.writeEndObject();
                    }
                }));
    }

    @RobotKeyword("Add Node") @ArgumentNames({"node_id", "version", "port", "password"}) @Override
    public synchronized void addNode(String nodeId, String version, String port, String password) {
        LibraryServer.putNode(SxpNode.createInstance(new NodeId(nodeId),
                new SxpNodeIdentityBuilder().setSourceIp(new IpAddress("0.0.0.0".toCharArray()))
                        .setCapabilities(Configuration.getCapabilities(Version.Version4))
                        .setEnabled(true)
                        .setName(SOURCE)
                        .setVersion(getVersion(version))
                        .setTcpPort(new PortNumber(Integer.parseInt(port)))
                        .setSecurity(new SecurityBuilder().setPassword(
                                password == null || password.isEmpty() ? null : password).build())
                        .setTimers(new TimersBuilder().setRetryOpenTime(5)
                                .setDeleteHoldDownTime(0)
                                .setReconciliationTime(0)
                                .build())
                        .build()));
    }

    /**
     * @param mode   Local Peer mode that will be checked
     * @param ip     Ip-address of remote peer
     * @param port   Port of remote peer
     * @param nodeId NodeId of local SXP device
     * @return If specified peer si connected
     */
    @RobotKeyword("Is Peer Connected") @ArgumentNames({"mode", "ip", "port", "node_id"})
    public synchronized boolean isPeerConnected(String mode, String ip, String port, String nodeId) {
        return LibraryServer.getNode(Objects.requireNonNull(nodeId))
                .getAllOnConnections()
                .stream()
                .anyMatch(c -> Objects.nonNull(c) && c.getMode().equals(getMode(mode))
                        && c.getDestination().getPort() == Integer.parseInt(port) && Objects.equals(
                        c.getDestination().getAddress().getHostAddress(), ip));
    }

    /**
     * @param nodeId NodeId of local SXP device
     * @return JSON formatted content of MasterDatabase
     * @throws JsonProcessingException If any error occurs during JSON generation
     */
    @RobotKeyword("Get Master Database") @ArgumentNames({"node_id"}) public synchronized String getMasterDatabase(
            String nodeId) throws JsonProcessingException {
        SxpNode node = LibraryServer.getNode(Objects.requireNonNull(nodeId));
        if (Objects.isNull(node))
            return "{\"output\":{\"binding\":[]}}";
        return "{\"output\":{\"binding\":" + pojoBindingsSerializer.writeValueAsString(
                node.getDomain(SxpNode.DEFAULT_DOMAIN).getMasterDatabase().getBindings()) + "}}";
    }

    /**
     * @param prefix Ip-prefix of binding
     * @param sgt    Security group of binding
     * @return MasterDatabase binding instance
     */
    private MasterDatabaseBinding getBinding(String prefix, int sgt) {
        MasterDatabaseBindingBuilder bindingBuilder = new MasterDatabaseBindingBuilder();
        bindingBuilder.setPeerSequence(new PeerSequenceBuilder().setPeer(new ArrayList<>()).build());
        bindingBuilder.setTimestamp(TimeConv.toDt(System.currentTimeMillis()));
        bindingBuilder.setSecurityGroupTag(new Sgt(sgt));
        bindingBuilder.setIpPrefix(new IpPrefix(Objects.requireNonNull(prefix).toCharArray()));
        return bindingBuilder.build();
    }

    /**
     * @param prefix Ip-prefix of binding
     * @param sgt    Security group of binding
     * @param nodeId NodeId of local SXP device
     * @return If binding was successfully added
     */
    @RobotKeyword("Add Binding") @ArgumentNames({"prefix", "sgt", "node_id"}) public synchronized boolean addBinding(
            String prefix, String sgt, String nodeId) {
        return !LibraryServer.getNode(Objects.requireNonNull(nodeId))
                .putLocalBindingsMasterDatabase(Collections.singletonList(getBinding(prefix, Integer.parseInt(sgt))),
                        SxpNode.DEFAULT_DOMAIN)
                .isEmpty();
    }

    /**
     * @param prefix Ip-prefix of binding
     * @param sgt    Security group of binding
     * @param nodeId NodeId of local SXP device
     * @return If binding was successfully removed
     */
    @RobotKeyword("Delete Binding") @ArgumentNames({"prefix", "sgt", "node_id"})
    public synchronized boolean deleteBinding(String prefix, String sgt, String nodeId) {
        return !LibraryServer.getNode(Objects.requireNonNull(nodeId))
                .removeLocalBindingsMasterDatabase(Collections.singletonList(getBinding(prefix, Integer.parseInt(sgt))),
                        SxpNode.DEFAULT_DOMAIN)
                .isEmpty();
    }

    @Override public synchronized void close() {
        //NOP
    }
}
