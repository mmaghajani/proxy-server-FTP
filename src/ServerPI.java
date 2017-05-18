import java.io.*;
import java.net.Socket;

/**
 * Created by mma on 5/18/17.
 */
public class ServerPI implements Runnable {
    Socket connection;
    boolean loggedIn = false;
    String username = null;
    String password = null;

    public ServerPI(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        BufferedReader inFromClient = null;
        try {
            inFromClient = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(
                    connection.getOutputStream());
            while (true) {
                String clientSentece = inFromClient.readLine();
                String command = clientSentece.split("\\s")[0];
                String parameter = clientSentece.split("\\s")[1];
                switch (command) {
                    case "USER":
                        username = parameter;
                        if (username.equals("root"))
                            outToClient.writeBytes("331 " + Constants.CODE_331);
                        else
                            outToClient.writeBytes("332 " + Constants.CODE_332);
                        break;
                    case "PASS":
                        password = parameter;
                        if ( username.equals("root") && password.equals("toor")){
                            outToClient.writeBytes("230 " + Constants.CODE_230);
                            loggedIn = true;
                        }else{
                            outToClient.writeBytes("332 " + Constants.CODE_332);
                        }
                        break;
                    case "RMD":
                        if ( loggedIn == true){
                            File index = new File("/files");
                            String[]entries = index.list();
                            for(String s: entries){
                                File currentFile = new File(index.getPath(),s);
                                currentFile.delete();
                            }
                            outToClient.writeBytes("200 " + Constants.CODE_200);
                        }else{
                            outToClient.writeBytes("530 " + Constants.CODE_530);
                        }
                        break;
                    case "DELE":
                        break;
                    case "RETR":
                        break;
                    case "LIST":
                        break;
                    case "QUIT":
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
