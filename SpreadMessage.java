import daj.Message;

public class SpreadMessage extends Message {
    public static final int JOIN_REQ = 1;
    public static final int JOIN_OK = 2;
    public static final int LEAVE_REQ = 3;
    int type;
    int node_id;
    
    public SpreadMessage(int type, int node_id) {
        this.type = type;
        this.node_id = node_id;
    }
    
    public int getType() {
        return this.type;
    }
    
    public int getNodeId() {
        return this.node_id;
    }    
    
    public String getText() {
        switch(this.type) {
            case JOIN_REQ:
                return "Join Request";
            case JOIN_OK:
                return "Join OK: " + node_id;                
            case LEAVE_REQ:
                return "Leave Request";
            default:
                return "Unknown";
        }
    }
}