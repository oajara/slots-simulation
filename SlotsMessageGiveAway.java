public class SlotsMessageGiveAway extends SlotsMessage {
    private int[] indexes;

    public SlotsMessageGiveAway(int[] indexes, int senderId) {
        super(senderId);
        this.indexes = indexes;
    }

    public int[] getIndexes() {
        return indexes;
    }
    
}
