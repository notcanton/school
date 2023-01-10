import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Main {

    static Random rand = new Random();
    static ConfigParser configParser;
    static Mutex mutex;
    static ArrayList<Integer> responseTimeList;
    private static final Logger logger = Logger.getLogger("GLOBAL");

    public static void main(String[] args) {

        ConsoleHandler handler = new ConsoleHandler();
        responseTimeList = new ArrayList<>();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        Formatter formatter = new MyFormatter();  
        handler.setFormatter(formatter);  
        logger.setUseParentHandlers(false);

        // Set log output level
        logger.setLevel(Level.OFF);

        logger.info("Starting Node");
        
        try {
            configParser = new ConfigParser(args);
        } catch(Exception e) {
            logger.severe(e.getMessage());
            return;
        }

        logger.info("ID: " + configParser.nodeId);
        logger.info("Inter request delay: " + configParser.inter_request_delay);
        logger.info("CS Exec Time: " + configParser.cs_execution_time);
        logger.info("Num requests: " + configParser.num_requests);
        logger.info("Neighbors: " + configParser.nodeNeighbors.toString());

        mutex = new Mutex(configParser, false);

        try (FileOutputStream fileOutputStream = new FileOutputStream("./temp")) {
            FileChannel channel = fileOutputStream.getChannel();
            logger.info("Starting main loop");
            mainExecutionLoop(channel);
        } catch (Exception e ) {
            logger.severe(e.getMessage());
            return;
        }
        
        logger.info("Finished");
        logger.info("Waiting for others to finish");
        mutex.finish();
        logger.info("All nodes are done");
        logger.info("Node sent: " + mutex.getSentMsgCount() + " messages");

        Float totalResponseTime = (float) responseTimeList.stream().reduce(0, Integer::sum);
        Float averageResponseTime = (totalResponseTime / configParser.num_requests);
        
        try {
            FileOutputStream metricsLog = new FileOutputStream("./logs/metricsInterReq"+configParser.nodeId+".log", true);
            
            List<String> saveValues = Arrays.asList(String.valueOf(configParser.cs_execution_time), String.valueOf(mutex.getSentMsgCount()), String.valueOf(averageResponseTime));

            metricsLog.write((String.join(" ", saveValues) + "\n").getBytes());
            metricsLog.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Completed");
        return;
    }

    public static void mainExecutionLoop(FileChannel channel) throws IOException {
        for (int i = 0; i < configParser.num_requests; i++) {
            // Wait for next request
            long waitTime = (long) sampleExponential(configParser.inter_request_delay);
            logger.fine("Next Request in: " + waitTime + " ms");
            try {
                Thread.sleep(waitTime);
            } catch(Exception e) {
                logger.severe(e.getMessage());
                return;
            }

            // Send mutex reqeust
            logger.fine("Calling cs_enter");
            Integer requestStartTime = (int) System.currentTimeMillis();

            mutex.cs_enter();

            FileLock lock = channel.tryLock();

            if (lock == null) {
                logger.severe("UNABLE TO GET FILE LOCK");
            }

            // Wait for cs execution time
            waitTime = (long) sampleExponential(configParser.cs_execution_time);
            logger.fine("Entering CS for: " + waitTime + " ms");
            try {
                Thread.sleep(waitTime);
            } catch(Exception e) {
                logger.severe(e.getMessage());
                return;
            }

            if (lock != null)
                lock.release();
        
            // Exit mutex
            logger.fine("Calling cs_leave");
            mutex.cs_leave();
            Integer requestEndTime = (int) System.currentTimeMillis();

            Integer responseTime = requestEndTime - requestStartTime;  
            responseTimeList.add(responseTime);
        }
    }

    // https://stackoverflow.com/questions/29020652/java-exponential-distribution
    public static double sampleExponential(int mean) {
        return Math.log(1 - rand.nextDouble())/(- (1 / (double) mean));
    }
}


class MyFormatter extends Formatter {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();

        Level level = record.getLevel();
        if(level == Level.INFO) {
            builder.append(ANSI_GREEN);
        } else if(level == Level.WARNING) {
            builder.append(ANSI_YELLOW);
        } else if(level == Level.SEVERE) {
            builder.append(ANSI_RED);
        } else if(level == Level.FINE) {
            builder.append(ANSI_CYAN);
        } else if(level == Level.FINER) {
            builder.append(ANSI_PURPLE);
        } else {
            builder.append(ANSI_WHITE);
        }

        builder.append(record.getLevel() + ":\t");
        builder.append(record.getSourceClassName() + ":\t");
        builder.append(formatMessage(record));
        builder.append(ANSI_RESET);
        builder.append(System.lineSeparator());
        return builder.toString();
    }
}
