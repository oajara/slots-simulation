public class SpreadMessageJoin extends SpreadMessage {
    boolean[] activeNodes;

    public SpreadMessageJoin(int senderId) {
        super(senderId);
    }
    
    public SpreadMessageJoin(int senderId, boolean[] activeNodes) {
        super(senderId);
        this.activeNodes = activeNodes;
    }    

    public boolean[] getActiveNodes() {
        return activeNodes;
    }

    public void setActiveNodes(boolean[] activeNodes) {
        this.activeNodes = activeNodes;
    }
    
 
}
