import java.util.*;
import java.net.*;
import java.io.*;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.file.*;
import java.security.*;
import java.security.spec.*;

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
                String initial_message = in.readLine(); // Assume the first message should be the username of incoming user
                Client.updateUserList();
                System.out.println(initial_message);
                // TODO: check if the address match with the user
                boolean user_exist = false;
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    e.printStackTrace();
                }
                synchronized(Client.userInfo){
                    user_exist = Client.userInfo.get(initial_message)!=null;
                }
                if(initial_message != null && user_exist){
                        // start chatting session with another user
                        if(Client.chatSocket == null){
                            System.out.println("Connection accepted! Name is "+initial_message);
                            synchronized(Client.out){
                                synchronized(Client.in){
                                    // need help from server to create a session key
                                    //Client.sendMessage("/startChatWith "+initial_message);
                                    try{
                                        String request_payload = "/startChatWith "+initial_message;
                                        Client.sendMessage(request_payload);
                                        String response = Client.handleServerPayload(Client.in.readLine());
                                        if(response==null || response.equals("denied")){
                                            System.out.println("Request of session key denied");
                                        }
                                        String encoded_key_bytes = Client.RSAdecrypt(Base64.getDecoder().decode(response.getBytes()), Client.private_key);
                                        byte[] key_bytes = Base64.getDecoder().decode(encoded_key_bytes.getBytes());
                                        Client.sessionKey = new SecretKeySpec(key_bytes, "AES");
                                        if(Client.sessionKey == null){
                                            System.out.println("fail to generate session key");
                                            Client.chatSocket = null;
                                            Client.sessionKey = null;
                                            Client.sendMessage("/endChat");
                                        }
                                        else{
                                            System.out.println("successfully generated session key");
                                            System.out.println(encoded_key_bytes);
                                            Client.opposed_username = initial_message;
                                            Client.chatSocket = client;
                                        }
                                    }catch(Exception e){
                                        System.out.println("cannot create session key");
                                        Client.chatSocket = null;
                                        Client.sessionKey = null;
                                        Client.sendMessage("/endChat");
                                    }
                                }
                            }
                        }
                        else{
                            System.out.println("chat socket not null");
                        }
                }
                else{
                    System.out.println("User not exist!");
                    Client.chatSocket = null;
                    Client.sessionKey = null;
                    Client.sendCounter = 0;
                    Client.sendMessage("/endChat");
                }

            }catch(IOException e){
                e.printStackTrace();
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
