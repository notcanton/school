import java.util.HashMap;

public class NodeState {
    public HashMap<Integer, Integer> vectorClock;
    public boolean passive;
    public int nodeId;

    public NodeState(int nodeId, boolean passive, HashMap<Integer, Integer> vectorClock) {
        this.vectorClock = vectorClock;
        this.passive = passive;
        this.nodeId = nodeId;
    }

    public String toString() {
        return ("NodeId: " + this.nodeId + " IsPassive: " + this.passive + " VectorClock: " + this.vectorClock.toString()); 
    }
}
