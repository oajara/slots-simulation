import daj.Message;

public abstract class SlotsMessage extends Message {
    protected int senderId;
    
    public SlotsMessage(int senderId) {
        this.senderId = senderId;
    }
    
    public int getSenderId() {
        return senderId;
    }
    
    @Override
    public String getText() {
        return "Type : "+ this.getClass().toString();
    }    
}
