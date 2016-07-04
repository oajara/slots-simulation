import java.util.ArrayList;
import java.util.List;

public class SlotsMessagePutStatus extends SlotsMessage{
    
    Slot[] slotsTable;
    private List<SlotsMessageRequestFork> requestQueue;
    private List<SlotsMessageReplyFork> receivedReplies;
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

    public List<SlotsMessageRequestFork> getRequestQueue() {
        return requestQueue;
    }

    public void setRequestQueue(List<SlotsMessageRequestFork> requestQueue) {
        this.requestQueue = requestQueue;
    }

    public List<SlotsMessageReplyFork> getReceivedReplies() {
        return receivedReplies;
    }

    public void setReceivedReplies(List<SlotsMessageReplyFork> receivedReplies) {
        this.receivedReplies = receivedReplies;
    }
}
