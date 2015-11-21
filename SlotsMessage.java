import daj.Message;

public abstract class SlotsMessage extends Message {
    private int senderId;
    
    public SlotsMessage(int senderId) {
        this.senderId = senderId;
    }
    
    public int getSenderId() {
        return senderId;
    }
}
