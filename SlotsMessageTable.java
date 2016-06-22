/**
 * Created by fer on 11/06/16.
 */
public class SlotsMessageTable extends SlotsMessage {
    private Slot[] slotsTable;

    public SlotsMessageTable(int senderId, Slot[] slotsTable) {
        super(senderId);

        this.slotsTable = slotsTable;
    }

    public Slot[] getSlotsTable() {
        return slotsTable;
    }

    public void setSlotsTable(Slot[] slotsTable) {
        this.slotsTable = slotsTable;
    }
}
