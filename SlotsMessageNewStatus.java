public class SlotsMessageNewStatus extends SlotsMessage {

    public SlotsMessageNewStatus(int senderId) {
        super(senderId);
    }
    
    @Override
    public String getText() {
        return "New Status";
    }
}