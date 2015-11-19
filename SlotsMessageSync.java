public class SlotsMessageSync extends SlotsMessage {
    int[] slotsTable;
    
    public SlotsMessageSync(int[] table) {
        this.slotsTable = table;
    }
    
    public int[] getTable() {
        return this.slotsTable;
    }
    
    public String getText() {
        return "Slots Table Sync Message";
    }
}