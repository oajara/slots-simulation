public class SpreadMessageJoin extends SpreadMessage {
    int[] registeredNodes;

    public SpreadMessageJoin(int senderId) {
        super(senderId);
    }
    
    public SpreadMessageJoin(int senderId, int[] registeredNodes) {
        super(senderId);
        this.registeredNodes = registeredNodes;
    }    

    public int[] getRegisteredNodes() {
        return registeredNodes;
    }

    public void setRegisteredNodes(int[] registeredNodes) {
        this.registeredNodes = registeredNodes;
    }
    
    

    
}
