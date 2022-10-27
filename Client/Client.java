import java.net.*;
import java.util.*;
import java.io.*;

class Client{

    public static String username;
    public static int ServerPort = 8080;    

    public static void sendMessage(Socket client,Message msg) throws IOException{
        ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
        out.writeObject(msg);
        out.flush();
    }

    public static void main(String[] args) throws IOException{
        username = args[0];
        Socket socket = new Socket("localhost", ServerPort);
        Scanner in = new Scanner(System.in);
        boolean exit = false;
        while(!exit){
            String content = in.nextLine();
            if(content.equals("exit")){
                exit = true;
            }
            else{
                Message msg = new Message(username,content);
                sendMessage(socket, msg);
            }
        }
    }

}