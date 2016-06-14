
public class SlotsMessageRequest extends SlotsMessage {

    public SlotsMessageRequest(int senderId) {
        super(senderId);
    }

    @Override
    public String getText() {
        return "Sender: "+this.getSenderId()+"\nRequest Message\nNeed: 1\n";
    }
}

