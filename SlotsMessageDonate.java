
public class SlotsMessageDonate extends SlotsMessage {
    int quantity;
    int destination;
    
    public SlotsMessageDonate(int quantity, int destination_node) {
        this.quantity = quantity;
        this.destination = destination_node;
    }
    
    public int getQuantity() {
        return this.quantity;
    }
    
    public int getDestination() {
        return this.destination;
    }    
    
    @Override
    public String getText() {
        return "Donation Message of ["+this.quantity+"] slots to node ["+this.destination+"]";
    }
}


