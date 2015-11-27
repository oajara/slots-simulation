public class SpreadMessageJoin extends SpreadMessage {
    boolean[] registeredNodes;

    public SpreadMessageJoin(int senderId) {
        super(senderId);
    }
    
    public SpreadMessageJoin(int senderId, boolean[] registeredNodes) {
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
