import java.io.*;
import java.util.*;

import javax.crypto.Cipher;

import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;

class Server{

    public static ServerSocket serverSocket;
    public static int port=8080;
    public static Set<String> user_list = new HashSet<>();
    public static HashMap<String, String> status = new HashMap<>();
    public static HashMap<String, String> client_address = new HashMap<>();
    public static HashMap<String, PublicKey> pubkey_list = new HashMap<>();

    public static PublicKey getPublicKey(String username){
        try{
            byte[] keyBytes = Files.readAllBytes(Paths.get("./Account/"+username+"/public_key.der"));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }catch(Exception e){
            System.out.println("Cannot fetch public key of "+username);
        }
        return null;
    }

    public static byte[] encrypt(String text, PublicKey key){
        byte[] cipherText = null;
        try{
            final Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipherText = cipher.doFinal(text.getBytes());
        }catch(Exception e){
            e.printStackTrace();
        }
        return cipherText;
    }

    public static void createUserFolder(){
        File dir = new File("./Account");
        if(!dir.exists())
            dir.mkdirs();
    }

    public static void getUserList(){
        File dir = new File("./Account");
        for(File file: dir.listFiles()){
            if(file.isDirectory()){
                String username = file.getName();
                pubkey_list.put(username, getPublicKey(username));
                System.out.println(username);
                user_list.add(username);
                status.put(username, "offline");
            }
        }
    }

    public static void main(String[] args) throws IOException{
        createUserFolder();
        getUserList();
        serverSocket = new ServerSocket(port);
        while(true){
            Socket client;
            try{
                client = serverSocket.accept();
                String client_address = client.getRemoteSocketAddress().toString();
                String hostname = client_address.split(":")[0].substring(1);
                String port = client_address.split(":")[1];
                System.out.println("Received connection from:"+hostname+":"+port);
                ServerThread thread = new ServerThread(client);
                new Thread(thread).start();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}