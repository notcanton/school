import java.io.Serializable;
import java.util.HashMap;

public class Message implements Serializable{
    public int authorId;
    public String value;
    volatile public HashMap<Integer, Integer> vectorClock;
    public boolean passive;

    public Message(int authorId, HashMap<Integer, Integer> vectorClock) {
        this.authorId = authorId;
        this.vectorClock = vectorClock;
        value = new String();
    }

    public Message(int authorId, String value, HashMap<Integer, Integer> vectorClock) {
        this.authorId = authorId;
        this.vectorClock = vectorClock;
        this.value = value;
    }

    public Message(int authorId, String value, HashMap<Integer, Integer> vectorClock, boolean passive) {
        this.authorId = authorId;
        this.vectorClock = vectorClock;
        this.value = value;
        this.passive = passive;
    }
}
