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
import javax.xml.stream.XMLStreamException;
import org.opendaylight.sxp.restconfclient.DSType;
import org.opendaylight.sxp.restconfclient.JsonDeserializer;
import org.opendaylight.sxp.restconfclient.RestconfClient;
import org.opendaylight.sxp.restconfclient.UserCredentials;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.reporters.Files;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

/**
 *
 * @author Martin Dindoffer
 */
public class SystemTests {

    private static final Logger LOG = LoggerFactory.getLogger(SystemTests.class);
    private static final String SXP_TOPO_URL = "network-topology:network-topology/topology/sxp";
    private RestconfClient configClient, operationalClient;
    private Genson genson;
    private JsonDeserializer jsonDeserializer;

    private static final String DUMMY_NODE = "{\n" +
//"    \"network-topology\":{\n" +
"	\"topology\": [\n" +
"		{\n" +
"			\"topology-id\": \"sxp\",\n" +
"			\"node\": [\n" +
"				{\n" +
"					\"node-id\": \"1.1.5.1\",\n" +
"					\"sxp-node:enabled\": true,\n" +
"					\"sxp-node:timers\": {\n" +
"						\"hold-time-max\": 180,\n" +
"						\"hold-time\": 90,\n" +
"						\"hold-time-min\": 90,\n" +
"						\"hold-time-min-acceptable\": 120,\n" +
"						\"delete-hold-down-time\": 120,\n" +
"						\"keep-alive-time\": 30,\n" +
"						\"retry-open-time\": 10,\n" +
"						\"reconciliation-time\": 120\n" +
"					},\n" +
"					\"sxp-node:sxp-peer-groups\": {},\n" +
"					\"sxp-node:version\": \"version4\",\n" +
"					\"sxp-node:security\": {\n" +
"						\"password\": \"\"\n" +
"					},\n" +
"					\"sxp-node:mapping-expanded\": 0,\n" +
"					\"sxp-node:sxp-domains\": {\n" +
"						\"sxp-domain\": [\n" +
"							{\n" +
"								\"domain-name\": \"global\",\n" +
"								\"connections\": {},\n" +
"								\"master-database\": {}\n" +
"							}\n" +
"						]\n" +
"					},\n" +
"					\"sxp-node:description\": \"ODL SXP 1.1.5.1 Controller\",\n" +
"					\"sxp-node:tcp-port\": 64999,\n" +
"					\"sxp-node:source-ip\": \"127.0.0.1\"\n" +
"				}\n" +
"			]\n" +
"		}\n" +
"	]\n" +
//"    }\n" +
"}";

    @Test
    public void testGetTopo() throws XMLStreamException {
        Response resp = configClient.get(SXP_TOPO_URL, "node", "1.1.5.1");
        String respString = resp.readEntity(String.class);
        LOG.info("RESP content: {}", respString);
        NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> nn
                = jsonDeserializer.deserializeJson(respString, SxpPaths.TOPOLOGY_PATH);
        LOG.info("Normalized node {}", nn);
        Map.Entry<InstanceIdentifier<?>, DataObject> dataObject = jsonDeserializer.unmarshallNormalizedNode(nn);
        LOG.info("Data Object: {}", dataObject);
        NetworkTopology out = (NetworkTopology) dataObject.getValue();
        List<Topology> topologies = out.getTopology();
        LOG.info("{}", topologies);
    }

//    @Test
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
        jsonDeserializer = new JsonDeserializer();
    }

//    @AfterTest
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
