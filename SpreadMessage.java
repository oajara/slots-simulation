import daj.Message;

public class SpreadMessage extends Message {
    public static final int JOIN_REQ = 1;
    public static final int JOIN_OK = 2;
    public static final int LEAVE_REQ = 3;
    public static final int LEAVE_OK = 4;
    
    int type;
    int node_id;
    int[] data;
    
    public SpreadMessage(int type, int node_id) {
        this.type = type;
        this.node_id = node_id;
    }
    
    public SpreadMessage(int type, int node_id, int[] data) {
        this.type = type;
        this.node_id = node_id;
        this.data = data;
    }    
    
    public int getType() {
        return this.type;
    }
    
    public int getNodeId() {
        return this.node_id;
    } 
    
    public int[] getData() {
        return this.data;
    }        
    
    public String getText() {
        switch(this.type) {
            case JOIN_REQ:
                return "Join Request: " + node_id;       
            case JOIN_OK:
                return "Join OK: " + node_id;                
            case LEAVE_REQ:
                return "Leave Request: " + node_id;       
            case LEAVE_OK:
                return "Leave OK: "+ node_id;                 
            default:
                return "Unknown";
        }
    }
}