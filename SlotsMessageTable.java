/**
 * Created by fer on 11/06/16.
 */
public class SlotsMessageTable extends SlotsMessage {
    private Slot[] slotsTable;

    public SlotsMessageTable(int senderId, Slot[] table) {
        super(senderId);
        this.slotsTable = table;
    }

    public void setTable(Slot[] table){
        slotsTable = table;
    }
    public Slot[] getTable(){
        return slotsTable;
    }
}
