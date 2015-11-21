public class SpreadMessageLeave extends SpreadMessage {
    int[] registeredNodes;

    public SpreadMessageLeave(int senderId) {
        super(senderId);
    }
    
    public SpreadMessageLeave(int senderId, int[] registeredNodes) {
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
