import java.io.*;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.net.*;
import java.nio.file.*;
import java.security.*;

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
        //synchronized(Server.status){
            for(String name: Server.user_list){
                if(Server.status.getOrDefault(name, "offline").equals("idle")){
                    String original_content = name+" "+Server.client_address.get(name);
                    out.println(Server.createPayload(original_content, username));
                }
            }
            out.println(Server.createPayload("/over", username));
        //}
    }

    @Override
    public void run(){
        boolean exit = false;
        boolean logined = false;
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
                    if(logined){
                        System.out.println("Repetitive login! Illegal Operation.");
                        break;
                    }
                    System.out.println("Received Login Request from "+username+". verifying...");
                    // TODO : verifying credentials
                    PublicKey pubkey = Server.pubkey_list.getOrDefault(username, null);
                    boolean isLegit = false;
                    String challenge = java.util.UUID.randomUUID().toString();
                    if(pubkey != null){
                        byte[] encrypted_challenge = Server.encrypt(challenge, pubkey);
                        String encoded = Base64.getEncoder().encodeToString(encrypted_challenge);
                        out.println(encoded);
                        String response = in.readLine();
                        if(response!=null && response.equals(challenge)){
                            isLegit = true;
                            System.out.println(username+" is verified.");
                        }
                    }
                    if(isLegit){
                        // user is legit
                        synchronized(Server.status){
                            Server.status.put(username, "idle");
                            Server.encoded_key_bytes.put(username, new HashMap<>());
                            out.println("success");
                            // sending a hashkey to client
                            String random_hashkey = java.util.UUID.randomUUID().toString();
                            Server.hashkey.put(username, random_hashkey);
                            byte[] key_byte = Server.generateSessionKey();
                            Server.user_key.put(username, new SecretKeySpec(key_byte, "AES"));
                            out.println(Base64.getEncoder().encodeToString(Server.encrypt(random_hashkey, pubkey)));
                            out.println(Base64.getEncoder().encodeToString(Server.encrypt(Base64.getEncoder().encodeToString(key_byte), pubkey)));
                            logined = true;
                        }
                    }
                    else{
                        // user is not legit, end this connection
                        out.println("fail");
                        System.out.println(username+" is not legit. Shutting down this connection.");
                        exit = true;
                    }
                    continue;
                }
                /* Decrypt message and check HMAC value */
                String encoded_encrypted_content = content.split("::")[0];
                String hmac_received = content.split("::")[1];
                byte[] decryptText = null;
                try{
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, Server.user_key.get(username), new IvParameterSpec(new byte[16]));
                    decryptText = cipher.doFinal(Base64.getDecoder().decode(encoded_encrypted_content.getBytes()));
                }catch(Exception ce){
                    ce.printStackTrace();
                }
                String real_content = new String(decryptText);
                if(!Server.calculateHMAC(real_content, Server.hashkey.get(username)).equals(hmac_received)){
                    System.out.println("hmac not matched! Malicious activity detected!");
                    client.close();
                    break;
                }
                System.out.println(username+":"+real_content+". Integrity check: HMAC matched!");
                if(real_content.equals("/list")){
                    sendUserList();
                    continue;
                }
                else if(real_content.equals("/exit")){
                    exit = true;
                    System.out.println(username+" ended connection to server");
                    Server.status.put(username, "offline");
                }
                else if(real_content.startsWith("/startChatWith ")){
                    String targetName = real_content.split(" ")[1];
                    if(!Server.status.getOrDefault(username, "offline").equals("idle") || !Server.status.getOrDefault(targetName, "offline").equals("idle")){
                        out.println(Server.createPayload("denied", username));
                        continue;
                    }
                    synchronized(Server.status){
                        synchronized(Server.encoded_key_bytes){
                            if(Server.encoded_key_bytes.get(username).get(targetName)==null){
                                // generate a session key
                                byte[] session_key_byte = Server.generateSessionKey();
                                String encoded_keybyte = Base64.getEncoder().encodeToString(session_key_byte);
                                Server.encoded_key_bytes.get(targetName).put(username, encoded_keybyte);
                                byte[] encrypted_content = Server.encrypt(encoded_keybyte, Server.pubkey_list.getOrDefault(username, null));
                                out.println(Server.createPayload(Base64.getEncoder().encodeToString(encrypted_content), username));
                            }
                            else{
                                // already have a session key
                                String encoded_keybyte = Server.encoded_key_bytes.get(username).get(targetName);
                                Server.encoded_key_bytes.get(username).put(targetName, null);
                                byte[] encrypted_content = Server.encrypt(encoded_keybyte, Server.pubkey_list.getOrDefault(username, null));
                                out.println(Server.createPayload(Base64.getEncoder().encodeToString(encrypted_content), username));
                                Server.status.put(username, "busy");
                                Server.status.put(targetName, "busy");
                            }
                        }
                    }
                }
                else if(real_content.equals("/endChat")){
                    synchronized(Server.status){
                        Server.status.put(username,"idle");
                    }
                }
                else{
                    //System.out.println(username+":"+real_content + " [integrity check: HMAC matched]");
                }
        }
        }catch(Exception e){
            e.printStackTrace();
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
