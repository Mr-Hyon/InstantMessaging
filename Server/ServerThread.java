import java.util.*;
import java.net.*;
import java.io.*;

public class ServerThread implements Runnable {
    
    private Socket client;

    public ServerThread(Socket socket){
        this.client = socket;
    }

    @Override
    public void run(){
        while(true){
            try{
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                Message msg = (Message)in.readObject();
                System.out.println(msg.getContent());
            }catch(Exception e){
                System.out.println("Server Thread Error!");
            }
        }
    }
}
