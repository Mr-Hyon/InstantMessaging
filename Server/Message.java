import java.io.Serializable;

public class Message implements Serializable {
    
    public String content;

    public Message(String content){
        this.content = content;
    }

    public String getContent(){
        return this.content;
    }
}
