import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by mma on 5/18/17.
 */
public class Main {
    public static void  main(String args[]){
        ServerSocket welcomeSocket;
        try {
            welcomeSocket = new ServerSocket(8000);
            System.err.println("Welcoming socket in server was created!");
            while (true) {
                System.err.println("Waiting for connection on port "
                        + welcomeSocket.getLocalPort());
                Socket connectionSocket = welcomeSocket.accept();
                System.err.println("New Client From "
                        + connectionSocket.getInetAddress() + ":"
                        + connectionSocket.getLocalPort() + " connected to server!");
                new Thread(new ServerPI(connectionSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
