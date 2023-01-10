import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;
import java.io.IOException;

public class ConfigParser {
    private static final Logger logger = Logger.getLogger("GLOBAL");
    public int nodeId;
    public int totalNodes;
    public int inter_request_delay;
    public int cs_execution_time;
    public int num_requests;
    public int port;
    public HashMap<Integer, NodeInfo> nodeNeighbors = new HashMap<>(); // <node id, nodeinfo>

    public ConfigParser() {
        return;
    }

    public ConfigParser(String[] args) throws IOException{
        nodeId = Integer.parseInt(args[0]);
        port = Integer.parseInt(args[1]);
        totalNodes = Integer.parseInt(args[2]);
        inter_request_delay = Integer.parseInt(args[3]);
        cs_execution_time = Integer.parseInt(args[4]);
        num_requests = Integer.parseInt(args[5]);


        // The idea is that we send the neighbor details as _"hostName","portNumber"_...
        // Therefore, to get the map of hostname to portnumber, we perform this processing.
        String[] neighborsString = args[6].split("_");
        neighborsString = Arrays.copyOfRange(neighborsString, 1, neighborsString.length);

        for (String neighbor : neighborsString) {
            String[] neighborDetails = neighbor.split(",");
            int id = Integer.parseInt(neighborDetails[0]);
            String address = neighborDetails[1];
            int port = Integer.parseInt(neighborDetails[2]);

            // Skip own node
            if (nodeId == id) {
                continue;
            } 

            nodeNeighbors.put(id, new NodeInfo(id, address, port));
        }
    }

    @Override
    public String toString() {
        return String.valueOf(nodeId);
    }
}