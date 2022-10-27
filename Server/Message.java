import java.io.Serializable;

public class Message implements Serializable {
    
    public String content;
    public String username;

    public Message(String username, String content){
        this.username = username;
        this.content = content;
    }

    public String getUserName(){
        return this.username;
    }

    public String getContent(){
        return this.content;
    }
}
