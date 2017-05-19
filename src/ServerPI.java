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
    private String username = "";
    private String password = "";
    private int dataConnectionPort = 0 ;

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
                String parameter = "";
                if( (clientSentece.split("\\s")).length > 1)
                    parameter = clientSentece.split("\\s")[1];
                switch (command) {
                    case "USER":
                        if(!loggedIn) {
                            username = parameter;
                            if (username.equals("root")) {
                                outToClient.writeBytes("331 " + Constants.PASSWORD_REQUIRED);
                                Logger.getInstance().logForClient("USER",Constants.PASSWORD_REQUIRED, username);
                            }
                            else {
                                outToClient.writeBytes("332 " + Constants.NEED_ACCOUNT);
                                Logger.getInstance().logForClient("USER",Constants.NEED_ACCOUNT, username);
                            }
                        }else{
                            outToClient.writeBytes("230 " + Constants.LOGGED_IN);
                            Logger.getInstance().logForClient("USER",Constants.LOGGED_IN, username);
                        }
                        break;
                    case "PASS":
                        if(!loggedIn) {
                            password = parameter;
                            if (username.equals("root") && password.equals("toor")) {
                                outToClient.writeBytes("230 " + Constants.LOGGED_IN);
                                Logger.getInstance().logForClient("PASS",Constants.LOGGED_IN,username );
                                loggedIn = true;
                            } else if (username.equals("root") && !password.equals("toor")) {
                                Logger.getInstance().logForClient("PASS",Constants.PASSWORD_REQUIRED, username);
                                username = "";
                                outToClient.writeBytes("331 " + Constants.PASSWORD_REQUIRED);
                            } else {
                                Logger.getInstance().logForClient("PASS",Constants.NEED_ACCOUNT, username);
                                outToClient.writeBytes("332 " + Constants.NEED_ACCOUNT);
                            }
                        }else{
                            Logger.getInstance().logForClient("PASS",Constants.LOGGED_IN, username);
                            outToClient.writeBytes("230 " + Constants.LOGGED_IN);
                        }
                        break;
                    case "RMD":
                        if (loggedIn){
                            File index = new File(Constants.CACHE_FOLDER);
                            System.out.println(index);
                            File[] entries = index.listFiles();
                            System.out.println(entries);
                            if( entries != null ) {
                                for (File f : entries) {
                                    f.delete();
                                }
                            }
                            outToClient.writeBytes("200 " + Constants.OK);
                            Logger.getInstance().logForClient("RMD",Constants.OK, username);
                        }else{
                            Logger.getInstance().logForClient("RMD",Constants.NOT_LOGGED_IN, username);
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "DELE":
                        if(loggedIn){
                            String filename = parameter;
                            File index = new File(Constants.CACHE_FOLDER + filename);
                            if(index.exists()) {
                                index.delete();
                                Logger.getInstance().logForClient("DELE",Constants.OK, username);
                                outToClient.writeBytes("200 " + Constants.OK);
                            }else{
                                Logger.getInstance().logForClient("DELE",Constants.FILE_NOT_EXISTS, username);
                                outToClient.writeBytes("553 " + Constants.FILE_NOT_EXISTS);
                            }
                        }else{
                            Logger.getInstance().logForClient("DELE",Constants.NOT_LOGGED_IN, username);
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "RETR":
                        if(loggedIn){
                            String filename = parameter;
                            File index = new File(Constants.CACHE_FOLDER + filename);
                            if(index.exists()){
                                ServerDTP serverDTP = new ServerDTP(connection,dataConnectionPort);
                                try{
                                    serverDTP.sendFile(index);
                                    Logger.getInstance().logForClient("RETR",Constants.OK, username);
                                    outToClient.writeBytes("200 " + Constants.OK);
                                }catch (IOException e){
                                    Logger.getInstance().logForClient("RETR",Constants.OK, username);
                                    outToClient.writeBytes("425 " + Constants.CANT_OPEN_DATA_CONNECTION);
                                }
                            }else{
                                String request = "GET /~94131090/CN1_Project_Files/" + filename +
                                        " HTTP/1.1\r\n" +
                                        "Host: ceit.aut.ac.ir:80\r\n" +
                                        "Connection: Close\r\n"+
                                        "\r\n";

                                File file = new File(Constants.CACHE_FOLDER + filename);
                                file.createNewFile();

                                file = sendHTTPRequestToServerForGetFile(request,file);

                                if(file != null) {
                                    ServerDTP serverDTP = new ServerDTP(connection,dataConnectionPort);
                                    try{
                                        serverDTP.sendFile(file);
                                        Logger.getInstance().logForServer("RETR",Constants.OK, username);
                                        outToClient.writeBytes("200 " + Constants.OK);
                                    }catch (IOException e){
                                        e.printStackTrace();
                                        Logger.getInstance().logForServer("RETR",Constants.CANT_OPEN_DATA_CONNECTION, username);
                                        outToClient.writeBytes("425 " + Constants.CANT_OPEN_DATA_CONNECTION);
                                        System.out.println("128");
                                    }
                                }else{
                                    outToClient.writeBytes("425 " + Constants.CANT_OPEN_DATA_CONNECTION);
                                    Logger.getInstance().logForServer("RETR",Constants.CANT_OPEN_DATA_CONNECTION, username);
                                    System.out.println("132");
                                }
                            }
                        }else{
                            Logger.getInstance().logForClient("RETR",Constants.NOT_LOGGED_IN, username);
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
                                ServerDTP serverDTP = new ServerDTP(connection,dataConnectionPort);
                                try{
                                    serverDTP.sendString(response);
                                    Logger.getInstance().logForServer("LIST",Constants.OK, username);
                                    outToClient.writeBytes("200 " + Constants.OK);
                                }catch (IOException e){
                                    Logger.getInstance().logForServer("LIST",Constants.CANT_OPEN_DATA_CONNECTION, username);
                                    outToClient.writeBytes("425 " + Constants.CANT_OPEN_DATA_CONNECTION);
                                }
                            }else{
                                Logger.getInstance().logForServer("LIST",Constants.CANT_OPEN_DATA_CONNECTION, username);
                                outToClient.writeBytes("425 " + Constants.CANT_OPEN_DATA_CONNECTION);
                            }
                        }else{
                            Logger.getInstance().logForClient("LIST",Constants.NOT_LOGGED_IN, username);
                            outToClient.writeBytes("530 " + Constants.NOT_LOGGED_IN);
                        }
                        break;
                    case "PORT":
                        dataConnectionPort = Integer.parseInt(parameter);
                        if( dataConnectionPort > 1023 ){
                            Logger.getInstance().logForClient("PORT",Constants.OK, username);
                            outToClient.writeBytes("200 " + Constants.OK);
                        }else{
                            dataConnectionPort = 0;
                            Logger.getInstance().logForClient("PORT",Constants.CANT_OPEN_DATA_CONNECTION, username);
                            outToClient.writeBytes("425 " + Constants.CANT_OPEN_DATA_CONNECTION);
                        }
                        break ;
                    case "QUIT":
                        Logger.getInstance().logForClient("QUIT",Constants.OK, username);
                        connection.close();
                        break outerLoop;
                    default:
                        Logger.getInstance().logForClient(command,Constants.COMMAND_NOT_IMPLEMENTED, username);
                        outToClient.writeBytes("502 " + Constants.COMMAND_NOT_IMPLEMENTED);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private File sendHTTPRequestToServerForGetFile(String request , File file){
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(Constants.SERVER_ADDRESS);
            System.out.println(addr);
            Socket socket = null;
            try {
                socket = new Socket(addr, 80);
                DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
                outToServer.writeBytes(request);

                final FileOutputStream fileOutputStream = new FileOutputStream(file);
                final InputStream inputStream = socket.getInputStream();

                // Header end flag.
                boolean headerEnded = false;

                byte[] bytes = new byte[1024];
                int length;
                while ((length = inputStream.read(bytes)) != -1) {
                    // If the end of the header had already been reached, write the bytes to the file as normal.
                    if (headerEnded)
                        fileOutputStream.write(bytes, 0, length);

                        // This locates the end of the header by comparing the current byte as well as the next 3 bytes
                        // with the HTTP header end "\r\n\r\n" (which in integer representation would be 13 10 13 10).
                        // If the end of the header is reached, the flag is set to true and the remaining data in the
                        // currently buffered byte array is written into the file.
                    else {
                        for (int i = 0; i < 1021; i++) {
                            if (bytes[i] == 13 && bytes[i + 1] == 10 && bytes[i + 2] == 13 && bytes[i + 3] == 10) {
                                headerEnded = true;
                                fileOutputStream.write(bytes, i+4 , 1024-i-4);
                                break;
                            }
                        }
                    }
                }
                inputStream.close();
                fileOutputStream.close();
            } catch (Exception e) {
                return null;
//                e.printStackTrace();
            }
        }catch (Exception e){
            return null;
//            e.printStackTrace();
        }
        return file;
    }

    private String sendHTTPRequestToServer(String request) {
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(Constants.SERVER_ADDRESS);
            System.out.println(addr);
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
