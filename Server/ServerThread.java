import java.util.*;
import java.net.*;
import java.io.*;

public class ServerThread implements Runnable {
    
    private Socket client;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ServerThread(Socket socket){
        try{
            this.client = socket;
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void sendUserList(){
        out.println("User List:");
        synchronized(Server.user_list){
            for(String username: Server.user_list){
                out.println(username);
            }
            out.println("/over");
        }
    }

    @Override
    public void run(){
        boolean exit = false;
        try{
        while(!exit){
                String input = in.readLine();
                username = input.split("\\|")[0];
                String content = input.split("\\|")[1];
                if(content.equals("/login")){
                    System.out.println("Received Login Request from "+username);
                    // TODO : verifying credentials
                    boolean isLegit = true;
                    File file = new File("./Account/"+username);
                    if(!file.exists()){
                        file.mkdirs();
                    }
                    if(isLegit){
                        out.println("success");
                    }
                    continue;
                }
                if(content.equals("/list")){
                    sendUserList();
                    continue;
                }
                else if(content.equals("/exit")){
                    exit = true;
                    System.out.println(username+" ended connection to server");
                }
                else{
                    System.out.println(username+":"+content);
                }
        }
        }catch(Exception e){
            System.out.println(username+" ended connection to server");
        }finally{
            closeSilently(client);
        }
    }

    public void closeSilently(Socket s){
        //System.out.println("close silently");
        if(s!=null){
            try{
                s.close();
            }catch(IOException e){
                // do nothing
            }
        }
    }
}
