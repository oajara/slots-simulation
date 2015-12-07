public class SlotsMessagePutStatus extends SlotsMessage{
    
    Slot[] slotsTable;
    boolean[] initializedNodes;
    int destination;
    
    @Override
    public String getText() {
        return "Sender: "+this.getSenderId()+"\n"
                + "Destiantion: "+this.getDestination()+"\n Put Status";
    }

    public SlotsMessagePutStatus(Slot[] slotsTable, boolean[] initializedNodes, int senderId, int destinationId) {
        super(senderId);
        this.slotsTable = slotsTable;
        this.initializedNodes = initializedNodes;
        this.destination = destinationId;
    }
    
    public void setStatus(Slot[] slotsTable, boolean[] initializedNodes) {
        this.slotsTable = slotsTable;
        this.initializedNodes = initializedNodes;
    }
    
    public Slot[] getSlotsTable() {
        return this.slotsTable;
    }
    
    public boolean[] getInitializedNodes() {
        return this.initializedNodes;
    }
    
    public int getDestination() {
        return this.destination;
    }    
}
