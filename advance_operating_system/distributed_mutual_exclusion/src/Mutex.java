import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Mutex {

    private static final Logger logger = Logger.getLogger("GLOBAL");
    private Map<Integer, ObjectOutputStream> neighborOOS;
    private Map<Integer, Socket> neighborSockets;
    private BlockingQueue<Message> taskQueue;
    private AtomicInteger scalarClock = new AtomicInteger(0);
    private AtomicInteger sentMsgCount = new AtomicInteger(0);
    private Semaphore csReady = new Semaphore(0);
    private Semaphore finished = new Semaphore(0);
    private List<String> startTimes = new ArrayList<>();
    private List<String> finishTimes = new ArrayList<>();
    public ConfigParser configParser;

    public Mutex(ConfigParser configParserObject, boolean ricart) {
        taskQueue = new LinkedBlockingQueue<>();
        neighborOOS = new HashMap<>();
        neighborSockets = new HashMap<>();
        configParser = configParserObject;

        Server s = new Server(configParser.port, configParser.nodeId, taskQueue);
        Thread serverThread =new Thread(s, "Server Thread");
        serverThread.start();

        connectToNeighbors(configParser.nodeNeighbors);

        Thread taskThread = null;

        if (ricart) {
            logger.info("Using ricart and aggarwala's mutual exclusion protocol");
            RicartTaskHandler rth = new RicartTaskHandler(taskQueue, neighborOOS, scalarClock, configParserObject, csReady, sentMsgCount, finished);
            taskThread = new Thread(rth, "Task Handler Thread");
        } else {
            logger.info("Using lamport's mutual exclusion protocol");
            TaskHandler th = new TaskHandler(taskQueue, neighborOOS, scalarClock, configParserObject, csReady, sentMsgCount, finished);
            taskThread = new Thread(th, "Task Handler Thread");
        }
        
        taskThread.start();

        return;
    }

    public void cs_enter() {
        logger.fine("Entering CS");

        // We add our REQUEST message to the task queue
        Message ownDummyRequestMessage = new Message(configParser.nodeId, "SELF-REQUEST", -1);
        taskQueue.add(ownDummyRequestMessage);
  

        // Wait until sempahore > 0 such that all reply messages are received
        try {
            csReady.acquire();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        //Log the time we enter CS for caluculating SD+E.
        if (startTimes.size() < 100) {
            List<String> values = Arrays.asList("S", String.valueOf(scalarClock.get()), String.valueOf(System.currentTimeMillis()));
            startTimes.add(String.join(" ", values));
        }
        
        logger.fine("I am allowed to execute CS!");
        return;
    }

    public void cs_leave() {
        //Log the time we exit CS for calulcating SD + E.
        if (finishTimes.size() < 100) {
            List<String> values = Arrays.asList("E", String.valueOf(scalarClock.get()), String.valueOf(System.currentTimeMillis()));
            finishTimes.add(String.join(" ", values));
        }

        Message releaseMessage = new Message(configParser.nodeId, "SELF-RELEASE", -1);
        taskQueue.add(releaseMessage);

        logger.fine("Completed sending release messages");
        return;
    }

    /**
     * 
     * @return the sent message count of this node
     */
    public int getSentMsgCount() {
        return sentMsgCount.get();
    }

    /**
     * Waits for neighbor nodes to finish
     */
    public void finish() {

        String msgValue = configParser.nodeId == 0 ? "REQUEST-FINISH" : "SELF-FINISH";
        Message message = new Message(configParser.nodeId, msgValue, -1);
        taskQueue.add(message);

        // Wait until sempahore > 0 such that all nodes are finished
        try {
            finished.acquire();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            FileOutputStream csTimeLog = new FileOutputStream("./logs/metricsInterReqSD"+configParser.nodeId+".log", true);
            
            csTimeLog.write(("D" + " " + String.valueOf(configParser.cs_execution_time) + " " + String.valueOf(configParser.inter_request_delay) + "\n").getBytes());

            for (String value: startTimes) {
                csTimeLog.write((value + "\n").getBytes());
            }

            for (String value: finishTimes) {
                csTimeLog.write((value + "\n").getBytes());
            }

            csTimeLog.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        



    }

    private void connectToNeighbors(HashMap<Integer, NodeInfo> nodeNeighbors) {
        for (Map.Entry<Integer, NodeInfo> entry : nodeNeighbors.entrySet()) {
            NodeInfo neighbor = entry.getValue();
            logger.info("Connecting to: " + neighbor.id);
            boolean connected = false;
            while (!connected) {
                try {
                    // Create socket TCP connection
                    Socket neighborSocket = new Socket(neighbor.address, neighbor.port);

                    // Create ObjectOutputStream
                    ObjectOutputStream neighborStream = new ObjectOutputStream(neighborSocket.getOutputStream());
                    neighborStream.flush();
                    
                    neighborSockets.put(neighbor.id, neighborSocket);
                    neighborOOS.put(neighbor.id, neighborStream);
                    connected = true;
                } catch (Exception e) {
                    try {
                        //e.printStackTrace();
                        Thread.sleep(1000);
                    } catch (InterruptedException ee) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return;
    }
}

class TaskHandler implements Runnable {
    protected static final Logger logger = Logger.getLogger("GLOBAL");
    protected BlockingQueue<Message> taskQueue;
    protected Map<Integer, ObjectOutputStream> neighborOOS;
    protected AtomicInteger scalarClock;          // techinically never used in Mutex so can be regular int
    protected PriorityQueue<Message> requestQueue;// Priority queue for L2 condition
    protected ConfigParser configParser;
    protected Message lastSelfRequest;            // The saved request message of curr node for L1 (also useful for  Ricart Agrawala)
    protected Set<Integer> repliedNeighborSet;    // L1 condition, id's of nodes with larger scaler clocks
    protected Semaphore csReady;                  // Tells cs_enter if the condition is met
    protected AtomicInteger sentMsgCount;
    protected Set<Integer> finishedNeighborSet;
    protected Semaphore finished;

    public TaskHandler(BlockingQueue<Message> taskQueue, 
                       Map<Integer, ObjectOutputStream> neighborOOS, 
                       AtomicInteger scalarClock, 
                       ConfigParser configParser,  
                       Semaphore csReady,
                       AtomicInteger sentMsgCount,
                       Semaphore finished) {
        this.taskQueue = taskQueue;
        this.neighborOOS = neighborOOS;
        this.scalarClock = scalarClock;
        this.requestQueue = new PriorityQueue<>();
        this.configParser = configParser;
        this.csReady = csReady;
        repliedNeighborSet = new HashSet<>();
        this.sentMsgCount = sentMsgCount;
        finishedNeighborSet = new HashSet<>();
        this.finished = finished;
    }
    
    
    public void run() {
        while (true) {
            Message receivedMessage;
            try {
                receivedMessage = taskQueue.take();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            logger.finer("Processing: " + receivedMessage);
            
            // If the message isn't a generated psuedo-message from the current node
            if (receivedMessage.authorId != configParser.nodeId){
                // Update clock on receive
                scalarClock.set(Integer.max(scalarClock.get(), receivedMessage.scalarClock) + 1);

                // L1 condition: current process receives a message from all other
                // nodes that have a larger timestamp than its own request
                // Save message author id to a set to check if L1 is satisfied at the end
                if (lastSelfRequest != null && receivedMessage.compareTo(lastSelfRequest) > 0) {
                    repliedNeighborSet.add(receivedMessage.authorId);
                }
            }

            switch (receivedMessage.value){
                case "SELF-REQUEST":
                    // Psuedo-message from cs_enter that initializes the first request message
                    // Saves and adds the generated request
                    Message requestMessage = broadcastMessage("REQUEST");
                    lastSelfRequest = requestMessage;
                    requestQueue.add(requestMessage);
                    break;
                case "REQUEST":
                    // Reply to neighbors
                    requestQueue.add(receivedMessage);
                    sendMessage(receivedMessage.authorId, "REPLY");

                    break;
                case "REPLY":
                    // do nothing
                    break;
                case "SELF-RELEASE":
                    // Psuedo-message from cs_leaves that broadcasts its release
                    try {
                        FileOutputStream timeLog = new FileOutputStream("./logs/log"+configParser.nodeId+".log", true);
                        timeLog.write(("E:"+scalarClock+"\n").getBytes());
                        timeLog.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    broadcastMessage("RELEASE");
                    requestQueue.removeIf(request -> (request.authorId==receivedMessage.authorId));
                    break;
                case "RELEASE":
                    // Remove request from priority queue
                    requestQueue.removeIf(request -> (request.authorId==receivedMessage.authorId));
                    break;
                // --------------- all values below here are for termination ------------------------
                // ------------------------ not for protocol ---------------------
                case "SELF-FINISH":
                    // Tell node 0, this node is done
                    sendMetaMessage(0, "REQUEST-FINISH");
                    logger.finer("Sending REQUEST-FINISH to node 0");
                    assert(configParser.nodeId != 0);
                    break;
                case "REQUEST-FINISH":
                    assert(configParser.nodeId == 0);
                    // Collects all finished nodes, and broadcast when all done
                    finishedNeighborSet.add(receivedMessage.authorId);
                    if (finishedNeighborSet.size() == configParser.nodeNeighbors.size() + 1) {
                        logger.finer("All nodes are finished, broadcasting ALL-FINISH");
                        broadcastMetaMessage("ALL-FINISH");
                        taskQueue.add(new Message(configParser.nodeId, "ALL-FINISH", -1));
                    }
                    break;
                case "ALL-FINISH":
                    // All nodes done, release the main thread
                    finished.release();
                    break;
                default:
                    logger.warning("Unknown messages detected in the network");
            }

            logger.finer("Request queue after: " + requestQueue);
            logger.finer("Last saved cs_enter request: " + lastSelfRequest);

            // Enter CS: L1 + L2 condition and there exists a saved request
            if (lastSelfRequest != null &&
                repliedNeighborSet.size() == configParser.nodeNeighbors.size() &&
                requestQueue.peek().authorId == configParser.nodeId) {

                try {
                    FileOutputStream timeLog = new FileOutputStream("./logs/log"+configParser.nodeId+".log", true);
                    timeLog.write(("S:"+scalarClock+"\n").getBytes());
                    timeLog.close();
                    lastSelfRequest = null;
                    repliedNeighborSet.clear();
                    csReady.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Message sendMessage(int recipientId, String messageValue) {
        Message message = new Message(configParser.nodeId, messageValue, scalarClock.incrementAndGet());
        logger.finer("Sending to " + recipientId + ": " + message);
        try {
            neighborOOS.get(recipientId).reset();
            neighborOOS.get(recipientId).writeObject(message);
            sentMsgCount.incrementAndGet();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    /**
     * Send message but doesn't increment message count
     * @param recipientId
     * @param messageValue
     * @return
     */
    public Message sendMetaMessage(int recipientId, String messageValue) {
        Message message = new Message(configParser.nodeId, messageValue, scalarClock.incrementAndGet());
        logger.finer("Sending to " + recipientId + ": " + message);
        try {
            neighborOOS.get(recipientId).reset();
            neighborOOS.get(recipientId).writeObject(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    protected Message broadcastMessage(String messageValue) {
        Message broadcastMessage = new Message(configParser.nodeId, messageValue, scalarClock.incrementAndGet());
        logger.finer("Broadcasting: " + broadcastMessage);


        for (Entry<Integer, NodeInfo> neighborEntry: configParser.nodeNeighbors.entrySet()) {
            ObjectOutputStream neighborStream = neighborOOS.get(neighborEntry.getKey());

            try {
                neighborStream.reset();
                neighborStream.writeObject(broadcastMessage);
                sentMsgCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return broadcastMessage;
    }

    /**
     * Broadcast message but doesn't increment message count
     * @param messageValue
     * @return
     */
    protected Message broadcastMetaMessage(String messageValue) {
        Message broadcastMessage = new Message(configParser.nodeId, messageValue, scalarClock.incrementAndGet());
        logger.finer("Broadcasting: " + broadcastMessage);


        for (Entry<Integer, NodeInfo> neighborEntry: configParser.nodeNeighbors.entrySet()) {
            ObjectOutputStream neighborStream = neighborOOS.get(neighborEntry.getKey());

            try {
                neighborStream.reset();
                neighborStream.writeObject(broadcastMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return broadcastMessage;
    }
}

class Server implements Runnable {
    private static final Logger logger = Logger.getLogger("GLOBAL");
    int port;
    int nodeId;
    BlockingQueue<Message> taskQueue;

    public Server(int port, int nodeId, BlockingQueue<Message> taskQueue) {
        this.port = port;
        this.nodeId = nodeId;
        this.taskQueue = taskQueue;
    }

    public void run() {
        try {
            logger.info("Creating Server channel");
            ServerSocket serverSocket = new ServerSocket(port);
            // Loop to allow all clients to connect
            logger.info("Server channel accepting requests");
            // You can change while(true) to while(coutner) to stop server thread
            // when all neighbors have connected
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Client connected");

                ListenChannel c = new ListenChannel(nodeId, clientSocket, taskQueue);
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
    private static final Logger logger = Logger.getLogger("GLOBAL");
    int nodeId;
    Socket clientSocket;                    // Socket listening to
    BlockingQueue<Message> taskQueue;       // task queue for main thread

    public ListenChannel(int nodeId, Socket socket, BlockingQueue<Message> taskQueue) {
        clientSocket = socket;
        this.taskQueue = taskQueue;
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
                    logger.finest("Recieved Message");
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
