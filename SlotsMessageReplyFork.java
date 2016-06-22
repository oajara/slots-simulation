/**
 * Created by fer on 11/06/16.
 */
public class SlotsMessageReplyFork extends SlotsMessage {
    private int timeStamp;

    public SlotsMessageReplyFork(int senderId, int timeStamp) {
        super(senderId);
        this.timeStamp = timeStamp;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }
}
