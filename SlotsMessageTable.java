/**
 * Created by fer on 11/06/16.
 */
public class SlotsMessageTable extends SlotsMessage {
    private int slotIndex;

    public SlotsMessageTable(int senderId, int slotIndex) {
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
