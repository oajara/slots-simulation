/**
 * Created by fer on 11/06/16.
 */
public class SlotsMessageTable extends SlotsMessage {
    private Slot[] slotsTable;
    private int slotIndex;

    public SlotsMessageTable(int senderId, Slot[] slotsTable) {
        super(senderId);
        this.slotsTable = slotsTable;
    }

    public SlotsMessageTable(int senderId, int slotIndex) {
        super(senderId);
        this.slotIndex = slotIndex;
    }

    public Slot[] getSlotsTable() {
        return slotsTable;
    }

    public void setSlotsTable(Slot[] slotsTable) {
        this.slotsTable = slotsTable;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }
}
