import java.net.*;
import java.util.*;
import java.io.*;

class Client{

    public static String username;
    public static int ServerPort = 8080;
    public static Socket socket;
    public static BufferedReader in;
    public static PrintWriter out;
    public static String seperator = "|";

    public static void sendMessage(String msg){
        out.println(username+ seperator +msg);
    }

    public static boolean sendLoginMessage(){
        try{
            out.println(username+ seperator +"/login");
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

    public static void main(String[] args){
        Scanner input_reader = new Scanner(System.in);
        System.out.println("Please enter your username:");
        username = input_reader.nextLine();
        if(username.contains("/")){
            System.out.println("Illegal User Name");
            System.exit(0);
        }
        File file = new File("./"+username);
        if(!file.exists()){
            file.mkdirs();
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
        boolean exit = false;
        while(!exit){
            System.out.print(">");
            String content = input_reader.nextLine();
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
                sendMessage(content);
                String res;
                try{
                    while(!(res = in.readLine()).equals("/over")){
                        System.out.println(res);
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
            else{
                //Message msg = new Message(username,content);
                sendMessage(content);
            }
        }
    }

}