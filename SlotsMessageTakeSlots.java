public class SlotsMessageTakeSlots extends SlotsMessage {
    int quantity;

    public SlotsMessageTakeSlots(int quantity, int senderId) {
        super(senderId);
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }
    
    
}
