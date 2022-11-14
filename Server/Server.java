import java.io.*;
import java.util.*;
import java.net.*;

class Server{

    public static ServerSocket serverSocket;
    public static int port=8080;
    public static Set<String> user_list = new HashSet<>();
    public static HashMap<String, String> status = new HashMap<>();

    public static void createUserFolder(){
        File dir = new File("./Account");
        if(!dir.exists())
            dir.mkdirs();
    }

    public static void getUserList(){
        File dir = new File("./Account");
        for(File file: dir.listFiles()){
            if(file.isDirectory()){
                String username = file.getName();
                System.out.println(username);
                user_list.add(username);
            }
        }
    }

    public static void main(String[] args) throws IOException{
        createUserFolder();
        getUserList();
        serverSocket = new ServerSocket(port);
        while(true){
            Socket client;
            try{
                client = serverSocket.accept();
                ServerThread thread = new ServerThread(client);
                new Thread(thread).start();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}