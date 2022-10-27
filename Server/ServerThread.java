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
        boolean exit = false;
        try{
        while(!exit){
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                Message msg = (Message)in.readObject();
                if(msg.getContent().equals("exit")){
                    exit = true;
                }
                else{
                    System.out.println(msg.getUserName()+":"+msg.getContent());
                }
        }
        }catch(Exception e){
            // do nothing
        }finally{
            closeSilently(client);
        }
    }

    public void closeSilently(Socket s){
        if(s!=null){
            try{
                s.close();
            }catch(IOException e){
                // do nothing
            }
        }
    }
}
