import java.net.*;
import java.util.*;

import javax.crypto.Cipher;

import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.io.*;

class Client{

    public static String username;
    public static int ServerPort = 8080;
    public static boolean isIdle = true;
    public static Socket socket = null; // this socket is between client and server
    public static Socket chatSocket = null; // this socket is between client and client
    public static String opposed_username = null; // 0 indicates chatSocket is null
    public static BufferedReader in;
    public static PrintWriter out;
    public static String seperator = "|";
    public static HashMap<String, String> userInfo = new HashMap<>(); // store the ip address of online user
    public static PrivateKey private_key;

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

    public static void sendMessage(String msg){
        synchronized(out){
            out.println(username+ seperator +msg);
        }
    }

    public static boolean sendLoginMessage(){
        try{
            out.println(username+ seperator +"/login");
            String challenge = in.readLine();
            byte[] decoded = Base64.getDecoder().decode(challenge.getBytes());
            String answer = RSAdecrypt(decoded, private_key);
            out.println(answer);
            String inMessage = in.readLine();
            System.out.println(inMessage);
            if(inMessage.equals("success")){
                return true;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }

    public static void updateUserList(){
        userInfo = new HashMap<>();
        synchronized(out){
            // Caution! Watch out for dead lock!
            synchronized(in){
                out.println(username + seperator + "/list");
                String res;
                try{
                    while(!(res = Client.in.readLine()).equals("/over")){
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

    public static void main(String[] args){
        Scanner input_reader = new Scanner(System.in);
        System.out.println("Please enter your username:");
        username = input_reader.nextLine();
        if(username.contains("/")){
            System.out.println("Illegal User Name");
            System.exit(0);
        }
        private_key = getPrivateKey(username);
        if(private_key==null){
            System.out.println("Please set up your rsa key!");
            System.exit(0);
        }
        // File file = new File("./"+username);
        // if(!file.exists()){
        //     file.mkdirs();
        // }
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
                //System.out.println("chat msg");
                try{
                    PrintWriter chat_out = new PrintWriter(chatSocket.getOutputStream(), true);
                    chat_out.println(content);
                    if(content.equals("/end")){
                        chat_out.close();
                        chatSocket = null;
                    }
                }catch(IOException e){
                    // do nothing
                }
                continue;
            }
            if(content.contains(seperator)){
                System.out.println("Illegal Character!");
                continue;
            }
            if(content.length()==0) continue;
            if(content.equals("/exit")){
                //Message msg = new Message(username,content);
                sendMessage(content);
                exit = true;
            }
            if(content.equals("/list")){
                userInfo = new HashMap<>();
                synchronized(out){
                    synchronized(in){
                        sendMessage(content);
                        String res;
                        try{
                            while( !(res = in.readLine()).equals("/over")){
                                System.out.println(res);
                                userInfo.put(res.split(" ")[0], res.split(" ")[1]);
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                }
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
                }
                else{
                    String ip_address = userInfo.get(target_name);
                    //synchronized(chatSocket){
                        try{
                            System.out.println(ip_address);
                            String hostname = ip_address.split(":")[0];
                            int port = Integer.parseInt(ip_address.split(":")[1]);
                            chatSocket = new Socket(hostname,port);
                            opposed_username = target_name;
                            isIdle = false;
                            PrintWriter chat_out = new PrintWriter(chatSocket.getOutputStream(), true);
                            chat_out.println(username);
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    //}
                }
            }
            else{
                //Message msg = new Message(username,content);
                sendMessage(content);
            }
        }
    }

}