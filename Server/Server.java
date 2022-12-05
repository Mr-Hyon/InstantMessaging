import java.io.*;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;

class Server{

    public static ServerSocket serverSocket;
    public static int port=8080;
    public static volatile Set<String> user_list = new HashSet<>();
    public static HashMap<String, String> status = new HashMap<>();
    public static HashMap<String, String> client_address = new HashMap<>();
    public static HashMap<String, PublicKey> pubkey_list = new HashMap<>();
    public static HashMap<String, HashMap<String, String>> encoded_key_bytes = new HashMap<>(); // between client and client
    public static HashMap<String, String> hashkey = new HashMap<>();    //  username: hashkey for integrity
    public static HashMap<String, SecretKey> user_key = new HashMap<>();   //  username: key for confidentiality

    /* This function will generate a 256-bit AES key as session key for clients to chat with each other */
    public static byte[] generateSessionKey() throws NoSuchAlgorithmException, NoSuchProviderException{
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(256);
        SecretKey key = kgen.generateKey();
        byte[] symmKey = key.getEncoded();
        return symmKey;
    }

    /* This function will fetch the public key of a specific user */
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

    /* This function will encrypt a message using given public key */
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

    /* This function will encrypt a message using given AES key in CBC mode*/
    public static byte[] AESencrypt(String text, SecretKey key){
        byte[] cipherText = null;
        try{
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
            cipherText = cipher.doFinal(text.getBytes());
        }catch(Exception e){
            e.printStackTrace();
        }
        return cipherText;
    }

    /* Create payload to send to client */
    /* payload format : "[encrypted_content]::[hmac value]" */
    public static String createPayload(String original_content, String username){
        String payload = null;
        try{
            String encrypted_content = Base64.getEncoder().encodeToString(Server.AESencrypt(original_content, Server.user_key.get(username)));
            String hmac = Server.calculateHMAC(original_content, Server.hashkey.get(username));
            payload = encrypted_content+"::"+hmac;
        }catch(Exception e){
            e.printStackTrace();
        }
        return payload;
    }

    /* This function will calculate a HMAC value based on given content and key to verify integrity */
    public static String calculateHMAC(String data, String key) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException{
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA512");
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(secretKeySpec);
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()));
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