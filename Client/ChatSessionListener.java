import java.util.*;
import java.net.*;
import java.io.*;

public class ChatSessionListener implements Runnable{

    public ChatSessionListener(){
        // nothing here
    }
    
    @Override
    public void run(){
        System.out.println("Listenning for chat session...");
        while(true){
            if(Client.chatSocket!=null){
                System.out.println("Chat session detected!");
                try{
                String opposed_username = Client.opposed_username;
                BufferedReader in = new BufferedReader(new InputStreamReader(Client.chatSocket.getInputStream()));
                System.out.println("-------------- Established chat session with "+opposed_username+" -------------------");
                boolean session_ended = false;
                while(!session_ended){
                    String message = in.readLine();
                    if(message == null)
                        break;
                    if(message.equals("/end")){
                        session_ended = true;
                    }
                    else{
                        System.out.println("From "+opposed_username+":"+message);
                    }
                 }
                System.out.println("-------------- Ended chat session -------------------");
                if(Client.chatSocket!=null){
                    Client.chatSocket = null;
                    Client.isIdle = true;
                }
                }catch(IOException e){
                    //e.printStackTrace();
                    System.out.println("-------------- Ended chat session -------------------");
                    Client.chatSocket = null;
                    try{
                        Thread.sleep(10);
                    }catch(Exception e2){
                        e2.printStackTrace();
                    }
                    // if(Client.chatSocket!=null){
                    //     try{
                    //         System.out.println("-------------- Ended chat session -------------------");
                    //         Client.chatSocket.close();
                    //         Client.isIdle = true;
                    //     }catch(IOException e2){
                    //         e2.printStackTrace();
                    //     }
                    // }
                }
            }
            else{
                //System.out.println("No chat session yet");
                try{
                    Thread.sleep(10);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
