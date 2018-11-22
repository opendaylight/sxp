/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.jst.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opendaylight.sxp.jst.util.NormalizedNodeUtils;
import org.opendaylight.sxp.jst.util.SxpSchemaPaths;
import org.opendaylight.sxp.jst.util.YangIdentifiers;
import org.opendaylight.sxp.restconfclient.DSType;
import org.opendaylight.sxp.restconfclient.JsonDeserializer;
import org.opendaylight.sxp.restconfclient.RestconfClient;
import org.opendaylight.sxp.restconfclient.UserCredentials;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martin Dindoffer
 */
public class SxpBindingRestconfClient {

    private static final Logger LOG = LoggerFactory.getLogger(SxpBindingRestconfClient.class);
    private static final String SXP_TOPO_URL = "network-topology:network-topology/topology/sxp";
    private final RestconfClient configClient, operationalClient;
    private final JsonDeserializer jsonDeserializer;

    public SxpBindingRestconfClient() {
        this.configClient = new RestconfClient.Builder()
                .setAcceptType(MediaType.APPLICATION_JSON_TYPE)
                .setContentType(MediaType.APPLICATION_XML_TYPE)
                .setCredentials(new UserCredentials("admin", "admin"))
                .setDsType(DSType.CONFIG)
                .setHost("localhost")
                .setPort(8181)
                .build();
        this.operationalClient = new RestconfClient.Builder()
                .setAcceptType(MediaType.APPLICATION_JSON_TYPE)
                .setContentType(MediaType.APPLICATION_XML_TYPE)
                .setCredentials(new UserCredentials("admin", "admin"))
                .setDsType(DSType.OPERATIONAL)
                .setHost("localhost")
                .setPort(8181)
                .build();
        this.jsonDeserializer = new JsonDeserializer();
    }

    public SxpNodeIdentity getConfigNode(String nodeId) {
        return getNode(nodeId, configClient);
    }

    public SxpNodeIdentity getOperationalNode(String nodeId) {
        return getNode(nodeId, operationalClient);
    }

    public Topology getConfigTopo() {
        return getTopo(configClient);
    }

    public Topology getOperationalTopo() {
        return getTopo(operationalClient);
    }

    public Response postNode(String nodePayload) {
        return configClient.post(nodePayload, SXP_TOPO_URL);
    }

    public Response deleteNode(String nodeId) {
        return configClient.delete(SXP_TOPO_URL, "node", nodeId);
    }

    public Response deleteTopo() {
        return configClient.delete(SXP_TOPO_URL);
    }

    public void waitForOperationalNodeAppearance(String nodeId, int msTimeout) throws InterruptedException, TimeoutException {
        Response nodeResp = operationalClient.get(SXP_TOPO_URL, "node", nodeId);
        int msSlept = 0;
        while (nodeResp.getStatusInfo() != Response.Status.OK) {
            if (msSlept >= msTimeout) {
                throw new TimeoutException("Timeout was reached waiting for appearance of node " + nodeId);
            }
            Thread.sleep(100);
            msSlept += 100;
            nodeResp = operationalClient.get(SXP_TOPO_URL, "node", nodeId);
        }
    }

    public void waitForEmptyTopo(int msTimeout) throws InterruptedException, TimeoutException {
        List<Node> nodes = getOperationalTopo().getNode();
        int msSlept = 0;
        while (nodes != null && nodes.isEmpty()) {
            if (msSlept >= msTimeout) {
                throw new TimeoutException("Timeout was reached waiting for an empty topo");
            }
            Thread.sleep(100);
            msSlept += 100;
            nodes = getOperationalTopo().getNode();
        }
    }

    private Topology getTopo(RestconfClient restClient) {
        Response resp = restClient.get(SXP_TOPO_URL);
        String respString = resp.readEntity(String.class);
        LOG.info("RESP content: {}", respString);
        NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> nn
                = jsonDeserializer.deserializeJson(respString, SxpSchemaPaths.NT_PATH);
        LOG.info("Normalized node {}", nn);
        Optional<NormalizedNode<?, ?>> sxpTopoOpt = NormalizedNodeUtils.pickTopology(nn, "sxp");
        LOG.info("Found sxp topo: {}", sxpTopoOpt);
        Map.Entry<InstanceIdentifier<?>, DataObject> topo = jsonDeserializer.unmarshallNormalizedNode(
                sxpTopoOpt.get(),
                YangIdentifiers.TOPO_LIST);
        Topology sxpTopo = (Topology) topo.getValue();
        LOG.info("Sxp topology: {}", sxpTopo);
        return sxpTopo;
    }

    private SxpNodeIdentity getNode(String nodeId, RestconfClient restClient) {
        Response resp = restClient.get(SXP_TOPO_URL, "node", nodeId);
        String respString = resp.readEntity(String.class);
        LOG.info("RESP content: {}", respString);
        NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> nn
                = jsonDeserializer.deserializeJson(respString, SxpSchemaPaths.TOPOLOGY_PATH);
        LOG.info("Normalized node {}", nn);
        Optional<NormalizedNode<?, ?>> sxpNodeOpt = NormalizedNodeUtils.pickNode(nn, nodeId);
        LOG.info("SXPNode optional {}", sxpNodeOpt);
        Map.Entry<InstanceIdentifier<?>, DataObject> sxpNode = jsonDeserializer.unmarshallNormalizedNode(
                sxpNodeOpt.get(),
                YangIdentifiers.SXP_NODE_LIST);
        SxpNodeIdentity sxpNodeIdentity = ((Node) sxpNode.getValue()).getAugmentation(SxpNodeIdentity.class);
        LOG.info("Sxp node: {}", sxpNodeIdentity);
        return sxpNodeIdentity;
    }

}
