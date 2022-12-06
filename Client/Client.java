import java.net.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.io.*;

class Client{

    public static String username;
    public static int ServerPort = 8080;
    public static boolean isIdle = true;
    public static Socket socket = null; // this socket is between client and server
    public static volatile Socket chatSocket = null; // this socket is between client and client
    public static String opposed_username = null;
    public static BufferedReader in;
    public static PrintWriter out;
    public static String seperator = "|";
    public static volatile HashMap<String, String> userInfo = new HashMap<>(); // store the ip address of online user
    public static PrivateKey private_key;
    public static PublicKey public_key;
    public static SecretKey sessionKey; // for client-client communication
    public static SecretKey serverKey;  // for client-server communication
    public static volatile int sendCounter = 0; // to use as sequence number during client-client communication
    public static String hashkey = null;

    /* read private key */
    public static PrivateKey getPrivateKey(String username){
        try{
            String filename = "./"+username+"/private_key.der";
            byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        }catch(Exception e){
            System.out.println("Please set up rsa key properly");
        }
        return null;
    }

    /* read public key */
    public static PublicKey getPublicKey(String username){
        try{
            String filename = "./"+username+"/public_key.der";
            byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }catch(Exception e){
            System.out.println("Please set up rsa key properly");
        }
        return null;
    }

    /* Used to decrypt sensitive content sent by Server using private key */
    public static String RSAdecrypt(byte[] text, PrivateKey key){
        byte[] decryptedText = null;
        try{
            final Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);
            decryptedText = cipher.doFinal(text);
        }catch(Exception e){
            e.printStackTrace();
        }
        return new String(decryptedText);
    }

    /* Used to decrypt normal content sent by Server using AES key */
    public static String AESdecrypt(byte[] text, SecretKey key){
        byte[] decryptedText = null;
        try{
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[16]));
            decryptedText = cipher.doFinal(text);
        }catch(Exception e){
            e.printStackTrace();
        }
        return new String(decryptedText);
    }

    /* This function will calculate a HMAC value based on given content and key to verify integrity */
    public static String calculateHMAC(String data, String key) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException{
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA512");
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(secretKeySpec);
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()));
    }

    /* Decrypt payload from server and verify HMAC */
    public static String handleServerPayload(String payload){
        try{
            String encrypted_content = payload.split("::")[0];
            String hmac = payload.split("::")[1];
            String decrypted_content = AESdecrypt(Base64.getDecoder().decode(encrypted_content), serverKey);
            if(!calculateHMAC(decrypted_content, hashkey).equals(hmac)){
                System.out.println("server hmac not matched!");
                System.exit(-1);
            }
            //System.out.println("Message from server:"+decrypted_content+". Integrity check:HMAC matched!");
            return decrypted_content;
        }catch(Exception e){
            System.out.println("fail to decode payload from server.");
        }
        return null;
    }

    /* This function can be used to send a message to server */
    public static void sendMessage(String msg){
        synchronized(out){
            String hmac = null;
            byte[] cipherText = null;
            try{
                hmac = calculateHMAC(msg, hashkey);
                try{
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.ENCRYPT_MODE, serverKey, new IvParameterSpec(new byte[16]));
                    cipherText = cipher.doFinal(msg.getBytes());
                }catch(Exception ce){
                    ce.printStackTrace();
                }
            }catch(Exception e){
                System.out.println("fail to calculate hmac value");
            }
            out.println(username+ seperator +Base64.getEncoder().encodeToString(cipherText)+"::"+hmac);
        }
    }

    /* This function can be used to receive a message from server */
    public static String receiveMessage(){
        String response = null;
        synchronized(in){
            try{
                response = handleServerPayload(in.readLine());
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        return response;
    }

    /* Initial handshake between client and server (login and credential check) */
    public static boolean sendLoginMessage(){
        try{
            synchronized(out){
                synchronized(in){
                out.println(username+ seperator +"/login");
                // decrypt challenge using private key to prove identity
                String challenge = in.readLine();
                byte[] decoded = Base64.getDecoder().decode(challenge.getBytes());
                String answer = RSAdecrypt(decoded, private_key);
                out.println(answer);
                String inMessage = in.readLine();
                System.out.println(inMessage);
                if(inMessage.equals("success")){
                    String encoded_encrypted_hashkey = in.readLine();
                    hashkey = RSAdecrypt(Base64.getDecoder().decode(encoded_encrypted_hashkey.getBytes()), private_key);
                    String encoded_encrypted_serverkey = in.readLine();
                    String decrypted_serverkey = RSAdecrypt(Base64.getDecoder().decode(encoded_encrypted_serverkey.getBytes()), private_key);
                    System.out.println("AES key for encrypting message to server: " + decrypted_serverkey);
                    serverKey = new SecretKeySpec(Base64.getDecoder().decode(decrypted_serverkey), "AES");
                    return true;
                }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }

    public static void updateUserList(){
        synchronized(userInfo){
        userInfo = new HashMap<>();
        synchronized(out){
            // Caution! Watch out for dead lock!
            synchronized(in){
                //out.println(username + seperator + "/list");
                sendMessage("/list");
                String res;
                try{
                    while(!(res = handleServerPayload(in.readLine())).equals("/over")){
                        System.out.println(res);
                        if(res.contains(":")){
                            userInfo.put(res.split(" ")[0], res.split(" ")[1]);
                        }
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
        }
    }

    public static void main(String[] args){
        Scanner input_reader = new Scanner(System.in);
        System.out.println("Please enter your username:");
        username = input_reader.nextLine();
        if(username.contains("/")){
            System.out.println("Illegal User Name");
            System.exit(0);
        }
        private_key = getPrivateKey(username);
        public_key = getPublicKey(username);
        if(private_key==null || public_key==null){
            System.out.println("Please set up your rsa key!");
            System.exit(0);
        }
        try{
            socket = new Socket("localhost", ServerPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }catch(IOException e){
            e.printStackTrace();
        }
        if(!sendLoginMessage()){
            // first time user, need to generate key pair
        }
        ConnectionListener listener = new ConnectionListener(socket.getLocalAddress().toString().substring(1)+":"+socket.getLocalPort());
        ChatSessionListener cListener = new ChatSessionListener();
        new Thread(listener).start();
        new Thread(cListener).start();
        boolean exit = false;
        while(!exit){
            String content = input_reader.nextLine();
            boolean isInChatSession = false;
            //synchronized(chatSocket){
                if(chatSocket!=null)
                    isInChatSession = true;
            //}
            if(isInChatSession){
                /* Client is participating a chat session, content of standard input will be sent to another Client instead of Server */
                try{
                    PrintWriter chat_out = new PrintWriter(chatSocket.getOutputStream(), true);
                    byte[] cipherText = null;
                    String hmac = null;
                    String original_content = content;
                    try{
                        // add sequence number for freshness check
                        content = (++sendCounter) + "::" + content;
                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        cipher.init(Cipher.ENCRYPT_MODE, sessionKey, new IvParameterSpec(new byte[16]));
                        cipherText = cipher.doFinal(content.getBytes());
                        hmac = calculateHMAC(content, Base64.getEncoder().encodeToString(sessionKey.getEncoded()));
                    }catch(Exception ce){
                        ce.printStackTrace();
                    }
                    chat_out.println(Base64.getEncoder().encodeToString(cipherText)+"::"+hmac);
                    if(original_content.equals("/end")){
                        chat_out.close();
                        chatSocket = null;
                        sendCounter = 0;
                    }
                }catch(IOException e){
                    // do nothing
                }
                continue;
            }
            sendCounter = 0;
            if(content.contains(seperator)){
                System.out.println("Illegal Character!");
                continue;
            }
            if(content.length()==0) continue;
            if(content.equals("/exit")){
                //Message msg = new Message(username,content);
                sendMessage(content);
                exit = true;
                System.exit(0);
            }
            if(content.equals("/list")){
                userInfo = new HashMap<>();
                synchronized(out){
                    synchronized(in){
                        sendMessage(content);
                        String res;
                        try{
                            while( !(res = handleServerPayload(in.readLine())).equals("/over")){
                                System.out.println(res);
                                userInfo.put(res.split(" ")[0], res.split(" ")[1]);
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                }
                continue;
            }
            if(content.startsWith("/chat ")){
                String target_name = content.split(" ")[1].trim();
                if(target_name.equals(username)){
                    System.out.println("You cannot start a chat session with yourself");
                    continue;
                }
                if(chatSocket!=null){
                    System.out.println("You are aleady in chat session!");
                    continue;
                }
                updateUserList();
                if(userInfo.get(target_name) == null){
                    System.out.println("User is busy or not exist!");
                    continue;
                }
                else{
                    String ip_address = userInfo.get(target_name);
                    //synchronized(chatSocket){
                        try{
                            System.out.println(ip_address);
                            String hostname = ip_address.split(":")[0];
                            int port = Integer.parseInt(ip_address.split(":")[1]);
                            Socket mySocket = new Socket(hostname,port);
                            opposed_username = target_name;
                            isIdle = false;
                            PrintWriter chat_out = new PrintWriter(mySocket.getOutputStream(), true);
                            chat_out.println(username);
                            synchronized(Client.out){
                                synchronized(Client.in){
                                    // need help from server to create a session key
                                    //Client.sendMessage("/startChatWith "+initial_message);
                                    try{
                                        String request_payload = "/startChatWith "+ target_name;
                                        Thread.sleep(200);
                                        sendMessage(request_payload);
                                        String response = handleServerPayload(in.readLine());
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
                                        }
                                        chatSocket = mySocket;
                                    }catch(Exception e){
                                        e.printStackTrace();
                                        System.out.println("cannot create session key");
                                        Client.chatSocket = null;
                                        Client.sessionKey = null;
                                        Client.sendMessage("/endChat");
                                    }
                                }
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                            Client.chatSocket = null;
                            Client.sessionKey = null;
                            Client.sendMessage("/endChat");
                        }
                    //}
                }
            }
            else{
                sendMessage(content);
            }
        }
    }

}