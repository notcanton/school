import java.io.ObjectOutputStream;
import java.net.*;

public class NodeInfo {
    public int id;
    public String address;
    public int port;
    public Socket socket;
    public ObjectOutputStream outputStream;

    public NodeInfo(int id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    // public void connect() {
    //     boolean connected = false;
    //         // Continously try if neighbor's server is not up yet
    //         while (!connected) {
    //             try {
    //                 // Create socket TCP connection
    //                 socket = new Socket(address, port);
    //                 // Create ObjectOutputStream
    //                 outputStream = new ObjectOutputStream(socket.getOutputStream());
    //                 connected = true;
                    
    //             } catch (Exception e) {
    //                 try {
    //                     Thread.sleep(1000);
    //                 } catch (InterruptedException ee) {
    //                     Thread.currentThread().interrupt();
    //                 }
    //             }
    //         }
    // }


}
