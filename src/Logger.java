import java.io.*;

/**
 * Created by mma on 5/19/17.
 */
public class Logger {
    private static Logger ourInstance = new Logger();

    File file;
    public static Logger getInstance() {
        return ourInstance;
    }

    private Logger() {
        file = new File("./log.txt");
    }

    public void logForServer(String command , String status , String username){
        String log = "Type: Connect To Server, Command: " + command + ", Status: " + status + ", Username: " + username;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file,true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            try {
                bw.append(log + "\n");
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void logForClient(String command, String status, String username){
        String log = "Type: Connect To Server, Command: " + command + ", Status: " + status + ", Username: " + username;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file , true);
            OutputStreamWriter bw =new OutputStreamWriter(fos);
            try {
                bw.append(log + "\n");
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
