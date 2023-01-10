import java.util.Arrays;
import java.util.HashMap;

public class ConfigParser {
    public int nodeId;
    public int port;
    public int minPerActive;
    public int maxPerActive;
    public int minSendDelay;
    public int snapshotDelay;
    public int maxNumber;
    public int totalNodes;
    public String configFileName;
    public HashMap<Integer, NodeInfo> nodeNeighbors = new HashMap<>(); // <node id, nodeinfo>

    public ConfigParser() {
        return;
    }

    public void parse(String[] args) {
        nodeId = Integer.parseInt(args[0]);
        port = Integer.parseInt(args[1]);
        minPerActive = Integer.parseInt(args[2]);
        maxPerActive = Integer.parseInt(args[3]);
        minSendDelay = Integer.parseInt(args[4]);
        snapshotDelay = Integer.parseInt(args[5]);
        maxNumber = Integer.parseInt(args[6]);
        totalNodes = Integer.parseInt(args[8]);
        configFileName = args[9];

        // The idea is that we send the neighbor details as _"hostName","portNumber"_...
        // Therefore, to get the map of hostname to portnumber, we perform this processing.
        String[] neighborsString = args[7].split("_");
        neighborsString = Arrays.copyOfRange(neighborsString, 1, neighborsString.length);

        for (String neighbor : neighborsString) {
            String[] neighborDetails = neighbor.split(",");
            int id = Integer.parseInt(neighborDetails[1]);
            String address = neighborDetails[2];
            int port = Integer.parseInt(neighborDetails[3]);
            nodeNeighbors.put(id, new NodeInfo(id, address, port));
        }
    }
}