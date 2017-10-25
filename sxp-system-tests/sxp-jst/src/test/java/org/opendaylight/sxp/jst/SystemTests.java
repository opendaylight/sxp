package org.opendaylight.sxp.jst;

import com.owlike.genson.Genson;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.opendaylight.sxp.restconfclient.DSType;
import org.opendaylight.sxp.restconfclient.RestconfClient;
import org.opendaylight.sxp.restconfclient.UserCredentials;
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
    private static final String SXP_TOPO_URL = "network-topology:network-topology/topology/sxp";
    private RestconfClient configClient, operationalClient;
    private Genson genson;

    @Test
    public void testNodeAddDeleteRaceCondition() throws IOException, InterruptedException {
        InputStream node1 = this.getClass().getResourceAsStream("/SXP-node-1.1.5.1.xml");
        String node1String = Files.readFile(node1);
        LOG.info("POSTing node 1.1.5.1");
        Response postResp = configClient.post(node1String, SXP_TOPO_URL);
        LOG.info("RESP: {}", postResp.getStatusInfo());
        LOG.info("RESP content: {}", postResp.readEntity(String.class));
        String nodeId = "1.1.5.1";
        waitForGetStatus(nodeId, Status.OK);
        LOG.info("Node found");

        LOG.info("Deleting node {}", nodeId);
        Response deleteResp = configClient.delete(SXP_TOPO_URL, "node", nodeId);
        LOG.info("RESP: {}", deleteResp.getStatusInfo());

        Response post2Resp = configClient.post(node1String, SXP_TOPO_URL);
        LOG.info("RESP: {}", post2Resp.getStatusInfo());
        LOG.info("RESP content: {}", post2Resp.readEntity(String.class));
    }

    private void waitForGetStatus(String nodeId, Status status) throws InterruptedException {
        Response nodeResp = operationalClient.get(SXP_TOPO_URL, "node", nodeId);
        while (nodeResp.getStatus() != status.getStatusCode()) {
            Thread.sleep(100);
            nodeResp = operationalClient.get(SXP_TOPO_URL, "node", nodeId);
        }
    }

    @BeforeClass
    public void init() throws Exception {
        genson = new Genson();
        configClient = new RestconfClient.Builder()
                .setAcceptType(MediaType.APPLICATION_JSON_TYPE)
                .setContentType(MediaType.APPLICATION_XML_TYPE)
                .setCredentials(new UserCredentials("admin", "admin"))
                .setDsType(DSType.CONFIG)
                .setHost("localhost")
                .setPort(8181)
                .build();
        operationalClient = new RestconfClient.Builder()
                .setAcceptType(MediaType.APPLICATION_JSON_TYPE)
                .setContentType(MediaType.APPLICATION_XML_TYPE)
                .setCredentials(new UserCredentials("admin", "admin"))
                .setDsType(DSType.OPERATIONAL)
                .setHost("localhost")
                .setPort(8181)
                .build();
    }

    @AfterTest
    public void cleanupTopo() {
        LOG.info("Cleaning SXP topo...");
        Response deleteResp = configClient.delete(SXP_TOPO_URL);
        LOG.info("Delete status: {}", deleteResp.getStatusInfo());
        waitForEmptyTopo();
    }

    public void waitForEmptyTopo() {
        Map<String, Object> topology = getTopo();
        while (!isTopoEmpty(topology)) {
            topology = getTopo();
        }
    }

    private Map<String, Object> getTopo() {
        Response getResp = operationalClient.get(SXP_TOPO_URL);
        String topoString = getResp.readEntity(String.class);
        Map<String, Object> topology = genson.deserialize(topoString, Map.class);
        return topology;
    }

    public boolean isTopoEmpty(Map<String, Object> topo) {
        List<Map> topos = (List) topo.get("topology");
        return topos.get(0).size() == 1;
    }

    @BeforeMethod
    public void preTestHook(Method method) {
        String testName = method.getName();
        LOG.info("Running test {}", testName);
    }

}
