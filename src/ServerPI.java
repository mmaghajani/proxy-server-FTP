import java.net.Socket;

/**
 * Created by mma on 5/18/17.
 */
public class ServerPI implements Runnable {
    Socket connection;
    public ServerPI(Socket connection){
        this.connection = connection;
    }
    @Override
    public void run() {

    }
}
