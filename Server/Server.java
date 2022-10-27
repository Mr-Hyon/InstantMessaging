import java.io.*;
import java.util.*;
import java.net.*;

class Server{

    public static ServerSocket serverSocket;
    public static int port=8080;

    public static void main(String[] args) throws IOException{
        serverSocket = new ServerSocket(port);
        while(true){
            Socket client;
            try{
                System.out.println("Server waiting for connection");
                client = serverSocket.accept();
                ServerThread thread = new ServerThread(client);
                new Thread(thread).start();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}