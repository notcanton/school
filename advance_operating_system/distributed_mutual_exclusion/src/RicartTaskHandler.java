import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class RicartTaskHandler extends TaskHandler {
    private Set<Integer> deferredReplyMessages;

    public RicartTaskHandler(BlockingQueue<Message> taskQueue, 
                       Map<Integer, ObjectOutputStream> neighborOOS, 
                       AtomicInteger scalarClock, 
                       ConfigParser configParser,  
                       Semaphore csReady,
                       AtomicInteger sentMsgCount,
                       Semaphore finished) {
        super(taskQueue, neighborOOS, scalarClock, configParser, csReady, sentMsgCount, finished);

        this.deferredReplyMessages = new HashSet<>();
    }
    
    @Override
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

                    break;
                case "REQUEST":
                    // Reply to neighbors
                    if (lastSelfRequest != null && receivedMessage.compareTo(lastSelfRequest) > 0) {
                        deferredReplyMessages.add(receivedMessage.authorId);
                    } else {
                        sendMessage(receivedMessage.authorId, "REPLY");
                    }

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

                    broadcastMessageToSubset(deferredReplyMessages, "REPLY");

                    deferredReplyMessages.clear();
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

            logger.finer("Last saved cs_enter request: " + lastSelfRequest);

            // Enter CS: L1 + L2 condition and there exists a saved request
            if (lastSelfRequest != null &&
                repliedNeighborSet.size() == configParser.nodeNeighbors.size()) {

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

    public Message broadcastMessageToSubset(Set<Integer> nodeSubset, String messageValue) {
        Message broadcastMessage = new Message(configParser.nodeId, messageValue, scalarClock.incrementAndGet());
        logger.finer("Broadcasting: " + broadcastMessage + "to Subset: " + nodeSubset.toString());


        for (Integer nodeId: nodeSubset){
            ObjectOutputStream neighborStream = neighborOOS.get(nodeId);

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
}