public class SlotsMessagePutStatus extends SlotsMessage{
    
    Slot[] slotsTable;
    boolean[] initializedNodes;
    
    @Override
    public String getText() {
        return "Put Status";
    }

    public SlotsMessagePutStatus(Slot[] slotsTable, boolean[] initializedNodes, int senderId) {
        super(senderId);
        this.slotsTable = slotsTable;
        this.initializedNodes = initializedNodes;
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
}
