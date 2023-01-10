import java.io.Serializable;

public class Message implements Serializable, Comparable<Message>{
    public int authorId;
    public String value;
    volatile public Integer scalarClock;

    public Message(int authorId, Integer scalarClock) {
        this.authorId = authorId;
        this.scalarClock = scalarClock;
        value = new String();
    }

    public Message(int authorId, String value, Integer scalarClock) {
        this.authorId = authorId;
        this.scalarClock = scalarClock;
        this.value = value;
    }

    @Override
    public int compareTo(Message otherMessage) {
        if (this.scalarClock - otherMessage.scalarClock != 0) {
            return this.scalarClock - otherMessage.scalarClock;
        } else {
            return this.authorId - otherMessage.authorId;
        }
    }

    @Override
    public String toString() {
        return "Message Contents " + String.valueOf(this.authorId) + " " + String.valueOf(this.scalarClock) + " " + String.valueOf(this.value);
    }
}
