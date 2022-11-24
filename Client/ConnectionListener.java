import java.util.*;
import java.net.*;
import java.io.*;

public class ConnectionListener implements Runnable {
    
    public ServerSocket serverSocket;

    public ConnectionListener(String local_address){
        System.out.println(local_address);
        int port_number = Integer.parseInt(local_address.split(":")[1]);
        try{
            serverSocket = new ServerSocket(port_number);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        while(true){
            Socket client = null;
            try{
                client = serverSocket.accept();
                System.out.println("Connection detected! Is it legal?");
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                //PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                // client is connected by another potential client, now he need to verify who is it
                Client.updateUserList();
                String initial_message = in.readLine(); // Assume the first message should be the username of incoming user
                // TODO: check if the address match with the user
                if(initial_message != null && Client.userInfo.get(initial_message)!=null){
                    // String ip_address = Client.userInfo.get(initial_message);
                    // String remote_ip_address = client.getRemoteSocketAddress().toString().substring(1);
                    // if( !ip_address.equals(remote_ip_address) ){
                    //     System.out.println("Illegal Connection Denied!");
                    //     // information not matched!
                    //     System.out.println("Correct address:"+ip_address);
                    //     System.out.println("Given address:"+remote_ip_address);
                    //     in.close();
                    //     client.close();
                    // }
                        // start chatting session with another user
                        if(Client.chatSocket == null){
                            Client.opposed_username = initial_message;
                            Client.chatSocket = client;
                            System.out.println("Connection accepted! Name is "+initial_message);
                        }
                }

            }catch(IOException e){
                if( client!=null){
                    try{
                        client.close();
                    }catch(IOException e2){
                        // do nothing
                    }
                }
            }
        }
    }
}
