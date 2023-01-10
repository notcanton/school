import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.lang.System;

import static java.util.stream.Collectors.joining;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Node");
        Random rand = new Random();

        ConfigParser configParser = new ConfigParser();
        configParser.parse(args);

        HashMap<Integer, Integer> vectorClock = new HashMap<>();
        for (int i = 0; i<configParser.totalNodes; i++) {
            vectorClock.put(i, 0);
        }

        System.out.println("Node Id: " + configParser.nodeId);
        System.out.println("The neighbors are" + configParser.nodeNeighbors.toString());
        System.out.println("Port: " + configParser.port);
        System.out.println(configParser.toString());

        // Queue of recieving task messages
        // If the queue is empty, queue.take() will block the thread until there is a task
        BlockingQueue<Message> taskQueue = new LinkedBlockingQueue<>();

        if (configParser.nodeId == 0) {
            try {
                taskQueue.put(new Message(0, "A", vectorClock));
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        //Initialize the variables to ensure that we can store the requires states for snapshot.
        Map<Integer, List<String>> channelStates = new HashMap<>();
        //The array for storing the vector clocks in each snapshot to store in the text file.
        // List<HashMap<Integer, Integer>> nodeStateSnapshots = new ArrayList<>();
        List<String> nodeStateSnapshots = new ArrayList<>();
        boolean sentMarker = false;
        Integer parentNode = null;
        int numMarkerRemaining = configParser.nodeNeighbors.size();
        boolean takingSnapshot = false;

        // only accessible to node 0
        List<List<NodeState>> globalSnapshots = new ArrayList<>();  // <globalSnapshots>
        List<NodeState> currentGlobalSnapshot = null; // <nodeId, vectorClock>

        // Initalize channelStates
        for(int neighborId : configParser.nodeNeighbors.keySet()) {
            channelStates.put(neighborId, new ArrayList<String>());
        }

        // Start server thread to listen to node connection
        // This is done to ensure that we can receive messages and subsequently switch
        // to active based on lock.
        Server s = new Server(configParser.port, configParser.nodeId, taskQueue, vectorClock);
        Thread thread1 = new Thread(s, "Thread 1");
        thread1.start();

        // Create socket to connect to each neighbor
        // Create ObjectOutputStream to send objects
        // Use arrays so we can randomly choose a neighbor later 
        Socket[] neighborSockets = new Socket[configParser.nodeNeighbors.size()];
        ObjectOutputStream[] neighborOIS = new ObjectOutputStream[neighborSockets.length];
        Map<Integer, Integer> nodeIdMapping = new HashMap<>();


        // variable to see if the node has sent halt messages to its neighbor
        // if it has sent halt message, the node itself will halt
        boolean sentHalt = false;
 
        int i = 0;
        for (Map.Entry<Integer, NodeInfo> entry : configParser.nodeNeighbors.entrySet()) {
            NodeInfo neighbor = entry.getValue();
            boolean connected = false;
            while (!connected) {
                try {
                    // Create socket TCP connection
                    neighborSockets[i] = new Socket(neighbor.address, neighbor.port);

                    // Create ObjectOutputStream
                    neighborOIS[i] = new ObjectOutputStream(neighborSockets[i].getOutputStream());
                    neighborOIS[i].flush();

                    nodeIdMapping.put(neighbor.id, i);
                    i++;
                    connected = true;
                } catch (Exception e) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ee) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        //This is to initiate the snapshot process from node 0.
        if (!sentMarker && configParser.nodeId == 0) {
            try {
                taskQueue.put(new Message(0, "marker", vectorClock));
                numMarkerRemaining++;
                channelStates.put(0, new ArrayList<String>());
            } catch(Exception e) {
                e.printStackTrace();
                return;
            }
        };

        System.out.println("Entering Passive");

        // Keep count of number of messages sent
        int sentMsg = 0;

        // MAIN EXECUTION LOOP
        while (true) {
            // Passive: wait to recieve task from queue
            Message task;
            try {
                task = taskQueue.take();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            List<String> channelMessages = channelStates.get(task.authorId);

            switch (task.value) {

                // Recieve Active Message:
                case "A":
                    if (sentMsg >= configParser.maxNumber) {                        
                        break;
                    }

                    channelMessages.add(task.value);

                    System.out.println("Entering Active");
                    sentMsg += sendApplicationMessages(neighborOIS, configParser, rand, vectorClock);

                    System.out.println("Entering Passive");
                    break;
                
                case "marker":
                    // Initiate snapshot
                    if (!takingSnapshot) {
                        // Initialize variables
                        System.out.println("This is the first marker message I have received. from: " + task.authorId);
                        parentNode = task.authorId; // parent node to converge to
                        takingSnapshot = true;      // start snapshoting window

                        // Add start to Channel states
                        for(Map.Entry<Integer, List<String>> entry : channelStates.entrySet()) {
                            List<String> currChannel = entry.getValue();

                            currChannel.add("<Snapshot>");
                        }
                        
                        // Broadcast to neighbors
                        System.out.println("Sending marker messages to my neighbors.");
                        sendMarkerMessages(neighborOIS, configParser.nodeId, vectorClock);

                        //Store the local state in the snapshots array.
                        nodeStateSnapshots.add(vectorClock
                                                .entrySet().stream()
                                                .map(e -> String.valueOf(e.getValue()))
                                                .collect(joining(" ")));

                        // Node 0 create new snapshot record
                        currentGlobalSnapshot = new ArrayList<NodeState>();
                        globalSnapshots.add(currentGlobalSnapshot);
                    }
                    // marker messages
                    else {
                        System.out.println("I have already taken my snapshot. This marker message is to store the channel state");
                    }

                    numMarkerRemaining--;

                    channelMessages.add("</Snapshot>");

                    System.out.println("The states of all channels when receiving this marker message is" + channelStates.toString());

                    // Recieved all marker messages, prepare converge cast message
                    if (numMarkerRemaining == 0) {
                        takingSnapshot = false;     // end snapshotting window
                        // note: I added numMarkerReamining here because of the additional message node 0 recieve when initalizing
                        numMarkerRemaining = configParser.nodeNeighbors.size(); // reset counter for next snapshot cycle

                        boolean passive = checkPassiveStatus(channelStates);

                        taskQueue.add(new Message(configParser.nodeId, "converge", vectorClock, passive));
                    }

                    break;

                // Relay converge to parent
                case "converge":
                    
                    // if not node 0, send converge message to parent
                    if (configParser.nodeId != 0) {
                        sendMessage(neighborOIS[nodeIdMapping.get(parentNode)], task);
                    }
                    // if node 0, store vector clocks
                    else {
                        System.out.println("Recieved converge message from " + task.authorId);
                        // TODO: add termination conditon from each converge 
                        NodeState taskNodeState = new NodeState(task.authorId, task.passive, task.vectorClock);
                        currentGlobalSnapshot.add(taskNodeState);
                    }
                    break;

                case "halt":
                    // if node has not sent halt message to its neighbor
                    if (!sentHalt) {
                        System.out.println("Halt request by " + task.authorId);
                        // send halt messages
                        for (int neighborInd = 0; neighborInd < neighborOIS.length; neighborInd++) {
                            sendMessage(neighborOIS[neighborInd], new Message(configParser.nodeId, "halt", vectorClock));
                        }
                        sentHalt = true;
                    }
                    break;

                default:
                    break;
            }

            if (configParser.nodeId == 0) {
                // Collected all converge messages
                //TODO: Add condition to check if node 0 is passive.
                if (currentGlobalSnapshot != null && currentGlobalSnapshot.size() == configParser.totalNodes) {
                    System.out.println("Final Snapshot:");
                    for (NodeState nodeState: currentGlobalSnapshot) {
                        System.out.println(nodeState.toString());
                    }

                    // TODO: check if global state is consistent
                    // for process i, c_i[i] is the larger or equal to all c_k[i]

                    for (int nodeIdx = 0; nodeIdx<configParser.totalNodes; nodeIdx++) {
                        // The currIdx used inside the lambda expression is expected to be final.
                        final int currNodeIdx = nodeIdx;
                        NodeState currNodeState = currentGlobalSnapshot.stream().filter(nodeState -> nodeState.nodeId==currNodeIdx).findAny().get();

                        //The value of c_i[i]
                        Integer selfTimeStamp = currNodeState.vectorClock.get(nodeIdx);

                        NodeState inconsistentNode = currentGlobalSnapshot.stream()
                                                        .filter(nodeState -> nodeState.vectorClock.get(currNodeIdx) > selfTimeStamp)
                                                        .findAny()
                                                        .orElse(null);
                        
                        if (inconsistentNode != null) {
                            System.out.println("The global state is inconsistent due to the following node " +  currentGlobalSnapshot.get(nodeIdx).toString() + "with nodeId " + nodeIdx);
                        }
                    }


                    // TODO: check if able to terminate
                    //Check if any of the node states has passive set to false
                    NodeState activeNode = currentGlobalSnapshot.stream().filter(nodeState -> (!nodeState.passive & nodeState.nodeId!=0) ).findAny().orElse(null);
                    System.out.println("The active node is" + activeNode);
                    //If any one of the nodes is active, we need to initiate the snapshot protocol again. 
                    if (activeNode != null) {
                        System.out.print("Trigerring the snapshot protocol from node 0 again because of an active node");
                        currentGlobalSnapshot.clear();
                        numMarkerRemaining++;
                        taskQueue.add(new Message(0, "marker", vectorClock));
                    }
                    // if there is no active node
                    else {
                        System.out.println("Termination detected, halting all nodes");
                        currentGlobalSnapshot.clear();
                        taskQueue.add(new Message(0, "halt", vectorClock));
                    }


                }

                // TODO: if terminate, send terminate message
            }
            // if the process has received halt request and sent it to its neighbors
            if (sentHalt) {
                //Before we halt we write vector clock of the snapshots into a text file.
                try {
                    //This creates the output file to write the values into.
                    PrintWriter snapshotsWriter = new PrintWriter(configParser.configFileName + "-" + configParser.nodeId + ".out", "UTF-8");

                    for (String vectorClockString: nodeStateSnapshots) {
                        snapshotsWriter.println(vectorClockString);
                    }
                    snapshotsWriter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // break out the while loop
                break;
            }
        }

        System.out.println("Node " + configParser.nodeId + " has halted");
        System.exit(0); // terminates all running programs and threads
    }

    /**
     * Sends active messages randomly to neighbors 
     * @param neighborOIS array of neighbor's ObjectOutputStream to send message through
     * @param configParser used to get min and max messages and node ID
     * @param rand RNG
     * @return number of messages sent
     */
    public static int sendApplicationMessages(ObjectOutputStream[] neighborOIS, ConfigParser configParser, Random rand, HashMap<Integer,Integer> vectorClock) {
        int sentMsg = 0;

        // Select number of messages
        int numMsg = rand.nextInt(configParser.maxPerActive - configParser.minPerActive) + configParser.minPerActive;
        // For each message
        for (int i = 0; i < numMsg; i++) {


            // Pick a neighbor at random
            int nextNeighbor = rand.nextInt(neighborOIS.length);
            ObjectOutputStream outputStream = neighborOIS[nextNeighbor];
            try {
                // Send active message
                synchronized(vectorClock) {
                    outputStream.reset();
                    vectorClock.compute(configParser.nodeId, (key, value) -> value + 1);
                    outputStream.writeObject(new Message(configParser.nodeId, "A", vectorClock));

                    System.out.println("Sent message to " + nextNeighbor + ": with \t" + vectorClock.toString());
                    // System.out.println("The vector clock sent along with the message is " + vectorClock.toString());
                }
                sentMsg++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return sentMsg;
    }

    /**
     * Send marker messages on all outgoing channels. 
     * @param neighborOIS: Array of neighbor's ObjectOutputStream to send message through.
     * @param authorId: NodeId of the node that is sending the message. 
     * @param vectorClock: Current vector clock of the system when the message is sent.
     * @return none
     */
    public static void sendMarkerMessages(ObjectOutputStream[] neighborOIS, Integer authorId, HashMap<Integer,Integer> vectorClock) {
        for (int i=0; i<neighborOIS.length; i++) {
            ObjectOutputStream neighborOutputStream = neighborOIS[i];
            
            //TODO: Confirm that we should not update the vector clock for send events.
            try {
                synchronized(vectorClock) {
                    neighborOutputStream.reset();
                    neighborOutputStream.writeObject(new Message(authorId, "marker", vectorClock));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return;
    }

    public static void sendMessage(ObjectOutputStream NodeOutputStream, Message message) {
        try {
            NodeOutputStream.reset();
            NodeOutputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return;
    }

    public static boolean checkPassiveStatus(Map<Integer, List<String>> channelStates) {

        /**
         * Condition: there are no application messages between the inital 
         * snapshot message and the marker message in a corresponding channel
         * 
         * Check if the last two messages are the inital token and the marker token 
         */

        for(List<String> channelMessages : channelStates.values()) {
            if(!channelMessages.get(channelMessages.size() - 1).equals("</Snapshot>") ||
               !channelMessages.get(channelMessages.size() - 2).equals("<Snapshot>")) {

                return false;
            }
        }

        return true;
    }

}

class Server implements Runnable {
    int port;
    int nodeId;
    BlockingQueue<Message> taskQueue;
    HashMap<Integer, Integer> vectorClock;

    public Server(int port, int nodeId, BlockingQueue<Message> taskQueue, HashMap<Integer,Integer> vectorClock) {
        this.port = port;
        this.nodeId = nodeId;
        this.taskQueue = taskQueue;
        this.vectorClock = vectorClock;
    }

    public void run() {
        try {
            System.out.println("Creating Server channel");
            ServerSocket serverSocket = new ServerSocket(port);
            // Loop to allow all clients to connect
            System.out.println("Server channel accepting requests");
            // You can change while(true) to while(coutner) to stop server thread
            // when all neighbors have connected
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected");

                ListenChannel c = new ListenChannel(nodeId, clientSocket, taskQueue, vectorClock);
                Thread thread = new Thread(c);
                thread.start();

                Thread.sleep(3000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}

class ListenChannel implements Runnable {
    int nodeId;
    Socket clientSocket;                    // Socket listening to
    BlockingQueue<Message> taskQueue;       // task queue for main thread
    HashMap<Integer,Integer> vectorClock;

    public ListenChannel(int nodeId, Socket socket, BlockingQueue<Message> taskQueue, HashMap<Integer,Integer> vectorClock) {
        clientSocket = socket;
        this.taskQueue = taskQueue;
        this.vectorClock = vectorClock;
        this.nodeId = nodeId;
    }

    public void run() {

        try {
            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
            Object obj;

            while (true) {
                // Continously read object from socket
                obj = inputStream.readObject();

                // If object is a Message, add to task queue
                if (obj instanceof Message) {
                    Message receivedMessage = (Message) obj;
                    System.out.println("Recieved Message");

                    if (receivedMessage.value.equals("A")) {
                        synchronized(vectorClock) {
                            System.out.println("The received clock from " + receivedMessage.authorId +" is\t" + receivedMessage.vectorClock.toString());
                            for (Entry<Integer, Integer> vectorClockEntry: receivedMessage.vectorClock.entrySet()) {
                                vectorClock.compute(vectorClockEntry.getKey(), 
                                                    (key, value) -> Math.max(value, vectorClockEntry.getValue()));
                            }
        
                            vectorClock.compute(nodeId, (key, value) -> value + 1);
                            System.out.println("Updated vector clock is\t\t" + vectorClock.toString());
                        }
                    }
                    taskQueue.put(receivedMessage);
                }
            }

        } catch (Exception e) {
            if (e instanceof EOFException) {
                return;
            }
            e.printStackTrace();
            return;
        }
    }
}
