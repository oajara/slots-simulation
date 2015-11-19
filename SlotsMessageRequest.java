
public class SlotsMessageRequest extends SlotsMessage {
    int quantity;
    
    public SlotsMessageRequest(int quantity) {
        this.quantity = quantity;
    }
    
    public int getQuantity() {
        return this.quantity;
    }
    
    @Override
    public String getText() {
        return "Request Message ["+this.quantity+"] slots";
    }
}

