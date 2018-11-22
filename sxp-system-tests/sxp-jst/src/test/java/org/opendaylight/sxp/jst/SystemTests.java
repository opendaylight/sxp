/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sxp.jst;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.core.Response;
import org.opendaylight.sxp.jst.client.SxpBindingRestconfClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.reporters.Files;

/**
 *
 * @author Martin Dindoffer
 */
@SuppressWarnings("unused")
final class SystemTests {
    private static final Logger LOG = LoggerFactory.getLogger(SystemTests.class);

    private SxpBindingRestconfClient sxpClient;

    SystemTests() {
        this.sxpClient = new SxpBindingRestconfClient();
    }

    void testNodeAddDeleteRaceCondition() throws IOException, InterruptedException, TimeoutException {
        InputStream node1 = SystemTests.class.getResourceAsStream("/SXP-node-1.1.5.1.xml");
        String nodeId = "1.1.5.1";
        String nodePayload = Files.readFile(node1);
        LOG.info("POSTing node {}", nodeId);
        Response postResp = sxpClient.postNode(nodePayload);
        LOG.info("RESP: {}", postResp.getStatusInfo());

        sxpClient.waitForOperationalNodeAppearance(nodeId, 5_000);
        LOG.info("Node found");

        LOG.info("Deleting node {}", nodeId);
        Response deleteNodeResp = sxpClient.deleteNode(nodeId);
        LOG.info("RESP: {}", deleteNodeResp.getStatusInfo());

        Response post2Resp = sxpClient.postNode(nodePayload);
        LOG.info("RESP: {}", post2Resp.getStatusInfo());
    }

    void cleanupTopo() throws InterruptedException, TimeoutException {
        LOG.info("Cleaning SXP topo...");
        Response deleteTopoResp = sxpClient.deleteTopo();
        LOG.info("Delete status: {}", deleteTopoResp.getStatusInfo());
        sxpClient.waitForEmptyTopo(5_000);
    }
}
