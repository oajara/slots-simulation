import daj.Message;

public abstract class SpreadMessage extends Message {
   int senderId;

    public SpreadMessage(int senderId) {
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