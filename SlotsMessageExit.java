/**
 * Created by fer on 11/06/16.
 */
public class SlotsMessageExit extends SlotsMessage {
    private int slotIndex;

    public SlotsMessageExit(int senderId, int slotIndex) {
        super(senderId);
        this.slotIndex = slotIndex;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }
}
