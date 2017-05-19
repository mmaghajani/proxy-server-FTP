import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by mma on 5/18/17.
 */
public class ServerPI implements Runnable {
    private Socket connection;
    private boolean loggedIn = false;
    private String username = null;
    private String password = null;

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
            outerLoop:while (true) {
                String clientSentece = inFromClient.readLine();
                String command = clientSentece.split("\\s")[0];
                String parameter = clientSentece.split("\\s")[1];
                switch (command) {
                    case "USER":
                        username = parameter;
                        if (username.equals("root"))
                            outToClient.writeBytes("331 " + Constants.PASSWORD_REQUIRED);
                        else
                            outToClient.writeBytes("332 " + Constants.NEED_ACCOUNT);
                        break;
                    case "PASS":
                        password = parameter;
                        if ( username.equals("root") && password.equals("toor")){
                            outToClient.writeBytes("230 " + Constants.LOGGED_IN);
                            loggedIn = true;
                        }else{
                            outToClient.writeBytes("332 " + Constants.NEED_ACCOUNT);
                        }
                        break;
                    case "RMD":
                        if (loggedIn){
                            File index = new File("/files");
                            String[]entries = index.list();
                            for(String s: entries){
                                File currentFile = new File(index.getPath(),s);
                                currentFile.delete();
                            }
                            outToClient.writeBytes("200 " + Constants.OK);
                        }else{
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "DELE":
                        if(loggedIn){
                            String filename = parameter;
                            File index = new File("/files/" + filename);
                            if(index.exists()) {
                                index.delete();
                                outToClient.writeBytes("200 " + Constants.OK);
                            }else{
                                outToClient.writeBytes("553 " + Constants.FILE_NAME_NOT_ALLOWED);
                            }
                        }else{
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "RETR":
                        if(loggedIn){
                            String filename = parameter;
                            File index = new File("/file/" + filename);
                            if(index.exists()){
                                outToClient.writeBytes("200 " + Constants.OK);
                                ServerDTP serverDTP = new ServerDTP(connection);
                                serverDTP.sendFile(index);
                            }else{
                                String request = "GET /~94131090/CN1_Project_Files/" + filename +
                                        " HTTP/1.1\r\n" +
                                        "Host: ceit.aut.ac.ir:80\r\n" +
                                        "Connection: Close\r\n"+
                                        "\r\n";
                                String response = sendHTTPRequestToServer(request);
                                if(response != null ) {
                                    outToClient.writeBytes("200 " + Constants.OK);
                                    ServerDTP serverDTP = new ServerDTP(connection);
                                    serverDTP.sendString(response);
                                }else{
                                    outToClient.writeBytes("425 " + Constants.CANT_OPEN_DATA_CONNECTION);
                                }
                            }
                        }else{
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "LIST":
                        if(loggedIn){
                            String request = "GET /~94131090/CN1_Project_Files/ HTTP/1.1\r\n" +
                                    "Host: ceit.aut.ac.ir:80\r\n" +
                                    "Connection: Close\r\n"+
                                    "\r\n";
                            String response = sendHTTPRequestToServer(request);
                            if(response != null ) {
                                outToClient.writeBytes("200 " + Constants.OK);
                                ServerDTP serverDTP = new ServerDTP(connection);
                                serverDTP.sendString(response);
                            }else{
                                outToClient.writeBytes("425 " + Constants.CANT_OPEN_DATA_CONNECTION);
                            }
                        }else{
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "QUIT":
                        break outerLoop;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String sendHTTPRequestToServer(String request) {
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName("http://ceit.aut.ac.ir");
            Socket socket = null;
            try {
                socket = new Socket(addr, 80);
                DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outToServer.writeBytes(request);
                // Receive an HTTP reply from the web server
                boolean loop = true;
                StringBuilder sb = new StringBuilder();
                while (loop) {
                    if (inFromServer.ready()) {
                        int i = 0;
                        while (i != -1) {
                            i = inFromServer.read();
                            sb.append((char) i);
                        }
                        loop = false;
                    }
                }
                socket.close();
                return sb.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // send an HTTP request to the web server

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }
}
