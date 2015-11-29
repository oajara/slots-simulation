
public class SlotsMessageMergeStatus extends SlotsMessage {

    public SlotsMessageMergeStatus(int senderId) {
        super(senderId);
    }
    
    
    
    @Override
    public String getText() {
        return "Sender: "+this.getSenderId()+"\nMerge Status";
    }
}