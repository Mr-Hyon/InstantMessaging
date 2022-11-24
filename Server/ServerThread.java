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

    public String getHostName(){
        return client.getRemoteSocketAddress().toString().split(":")[0].substring(1);
    }

    public int getPort(){
        return Integer.parseInt(client.getRemoteSocketAddress().toString().split(":")[1]);
    }

    public void sendUserList(){
        out.println("User List");
        synchronized(Server.status){
            for(String username: Server.user_list){
                if(Server.status.getOrDefault(username, "offline").equals("idle")){
                    out.println(username+" "+Server.client_address.get(username));
                }
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
                if(username == null){
                    username = input.split("\\|")[0];
                    synchronized(Server.client_address){
                        Server.client_address.put(username, client.getRemoteSocketAddress().toString().substring(1));
                    }
                }
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
                        synchronized(Server.status){
                            Server.status.put(username, "idle");
                            out.println("success");
                        }
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
                    Server.status.put(username, "offline");
                }
                else{
                    System.out.println(username+":"+content);
                }
        }
        }catch(Exception e){
            //e.printStackTrace();
            System.out.println(username+" ended connection to server");
            Server.status.put(username, "offline");
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
