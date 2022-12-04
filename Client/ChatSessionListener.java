import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

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
                    String hmac = message.split("::")[1];
                    message = message.split("::")[0];
                    //System.out.println(message);
                    if(message == null)
                        break;
                    byte[] decryptText = null;
                    try{
                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        cipher.init(Cipher.DECRYPT_MODE, Client.sessionKey, new IvParameterSpec(new byte[16]));
                        decryptText = cipher.doFinal(Base64.getDecoder().decode(message.getBytes()));
                    }catch(Exception ce){
                        ce.printStackTrace();
                    }
                    message = new String(decryptText);
                    // check integrity
                    try{
                        if(!Client.calculateHMAC(message, Base64.getEncoder().encodeToString(Client.sessionKey.getEncoded())).equals(hmac)){
                            System.out.println("HMAC not matched!");
                            Client.chatSocket = null;
                            break;
                        }
                    }catch(Exception e){
                        System.out.println("fail to check HMAC");
                        Client.chatSocket = null;
                        break;
                    }
                    //System.out.println(message);
                    if(message.equals("/end")){
                        session_ended = true;
                    }
                    else{
                        System.out.println("From "+opposed_username+":"+message +" [Integrity check: HMAC matched!]");
                    }
                 }
                System.out.println("-------------- Ended chat session -------------------");
                Client.sessionKey = null;
                if(Client.chatSocket!=null){
                    Client.chatSocket = null;
                    Client.isIdle = true;
                    Client.sendMessage("/endChat");
                }
                }catch(IOException e){
                    //e.printStackTrace();
                    System.out.println("-------------- Ended chat session -------------------");
                    Client.chatSocket = null;
                    Client.sessionKey = null;
                    Client.sendMessage("/endChat");
                    try{
                        Thread.sleep(10);
                    }catch(Exception e2){
                        e2.printStackTrace();
                    }
                }
            }
            else{
                //System.out.println("No chat session yet");
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
