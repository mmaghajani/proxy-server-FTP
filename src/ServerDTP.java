import java.io.*;
import java.net.Socket;

/**
 * Created by mma on 5/18/17.
 */
public class ServerDTP {
    private Socket connection;
    private int portNum;

    public ServerDTP(Socket connection, int portNum) {
        this.connection = connection;
        this.portNum = portNum;
    }

    public void sendString(String response) throws IOException {
        Socket clientSocket = null;
        clientSocket = new Socket(connection.getInetAddress(), portNum);
        DataOutputStream outToClient = new DataOutputStream(
                clientSocket.getOutputStream());
        outToClient.writeBytes(response + "\n");
        outToClient.flush();
        outToClient.close();
        clientSocket.close();
        System.err.println("Connection Closed!");

    }

    public void sendFile(File file) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        Socket clientSocket = null;

        clientSocket = new Socket(connection.getInetAddress(), portNum);
            System.out.println("Waiting...");
            try {
                // send file
                byte[] mybytearray = new byte[(int) file.length()];
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                bis.read(mybytearray, 0, mybytearray.length);
                os = clientSocket.getOutputStream();
                System.out.println("Sending " + file.getName() + "(" + mybytearray.length + " bytes)");
                os.write(mybytearray, 0, mybytearray.length);
                os.flush();
                System.out.println("Done.");
            } finally {
                if (bis != null) bis.close();
                if (os != null) os.close();
            }
    }

}
