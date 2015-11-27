public class SpreadMessageLeave extends SpreadMessage {
    boolean[] registeredNodes;

    public SpreadMessageLeave(int senderId) {
        super(senderId);
    }
    
    public SpreadMessageLeave(int senderId, boolean[] registeredNodes) {
        super(senderId);
        this.registeredNodes = registeredNodes;
    }    

    public boolean[] getRegisteredNodes() {
        return registeredNodes;
    }

    public void setRegisteredNodes(boolean[] registeredNodes) {
        this.registeredNodes = registeredNodes;
    }
    
    

    
}
