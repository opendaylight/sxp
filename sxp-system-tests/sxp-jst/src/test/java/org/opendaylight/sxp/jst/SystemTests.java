package org.opendaylight.sxp.jst;

import org.opendaylight.sxp.jst.client.SxpBindingRestconfClient;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.core.Response;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.reporters.Files;

/**
 *
 * @author Martin Dindoffer
 */
public class SystemTests {

    private static final Logger LOG = LoggerFactory.getLogger(SystemTests.class);

    private SxpBindingRestconfClient sxpClient;

    @BeforeClass
    public void init() throws Exception {
        this.sxpClient = new SxpBindingRestconfClient();
    }

    @BeforeMethod
    public void preTestHook(Method method) {
        String testName = method.getName();
        LOG.info("Running test {}", testName);
    }

    @Test
    public void testNodeAddDeleteRaceCondition() throws IOException, InterruptedException, TimeoutException {
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

    @AfterTest
    public void cleanupTopo() throws InterruptedException, TimeoutException {
        LOG.info("Cleaning SXP topo...");
        Response deleteTopoResp = sxpClient.deleteTopo();
        LOG.info("Delete status: {}", deleteTopoResp.getStatusInfo());
        sxpClient.waitForEmptyTopo(5_000);
    }

}
