public class SlotsMessageInitialized extends SlotsMessage{

    public SlotsMessageInitialized(int senderId) {
        super(senderId);
    }
    
    @Override
    public String getText() {
        return "Sender: "+this.getSenderId()+"\nInitialized";
    }
}
