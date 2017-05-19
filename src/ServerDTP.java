import java.io.*;
import java.net.Socket;

/**
 * Created by mma on 5/18/17.
 */
public class ServerDTP {
    private Socket connection;


    public ServerDTP(Socket connection){
        this.connection = connection;
    }

    public void sendString(String response){
        Socket clientSocket = null;
        try {
            clientSocket = new Socket(connection.getInetAddress(), connection.getPort());
            DataOutputStream outToClient = new DataOutputStream(
                    clientSocket.getOutputStream());
            outToClient.writeBytes(response+ "\n");
            clientSocket.close();
            System.err.println("Connection Closed!");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendFile(File file){
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        Socket clientSocket = null;
        try {
            clientSocket = new Socket(connection.getInetAddress() , connection.getPort());
            while (true) {
                System.out.println("Waiting...");
                try {
                    // send file
                    byte [] mybytearray  = new byte [(int)file.length()];
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    bis.read(mybytearray,0,mybytearray.length);
                    os = clientSocket.getOutputStream();
                    System.out.println("Sending " + file.getName() + "(" + mybytearray.length + " bytes)");
                    os.write(mybytearray,0,mybytearray.length);
                    os.flush();
                    System.out.println("Done.");
                }
                finally {
                    if (bis != null) bis.close();
                    if (os != null) os.close();
                }
            }
        }
        catch(Exception e) {
            System.err.println("can't establish connecton");
        }
    }


}
