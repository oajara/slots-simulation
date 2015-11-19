import daj.Message;
import daj.Program;

/**
 * Program for each node.
 */

public class ProgSpreadNode extends Program {
    int broadcasted = 0;
    private int[] registeredNodes = new int[SlotsDonation.MAX_NODES+1];
    
    public ProgSpreadNode() {
        /* no nodes at the beggining */
        for (int i = 1; i <= SlotsDonation.MAX_NODES; i++ ) {
            registeredNodes[i] = 0;
        }
    }   
    
    public void main() {
        println("Initializing...");
        receiveFromAll();
    }
    
    private void println(String str) {
        System.out.println("Node[SP]: "+str);
    }
    
    private void receiveFromAll() {
        int index = in().select();
        Message msg = in(index).receive();
        
        if (msg instanceof SpreadMessage) {
            println("Received Spread Message");
            handleSpreadMessage((SpreadMessage)msg);
        } else if (msg instanceof DonationMessage){
            println("Received Donation Message");
//            handleDonateMessage(msg);
        }
    }
    
    private void handleSpreadMessage(SpreadMessage msg) {
        switch(msg.getType()) {
            case SpreadMessage.JOIN_REQ:
                processJoin(msg.getNodeId());
                break;
            case SpreadMessage.LEAVE_REQ:
                println("Processing Spread LEAVE Message");
                break;
        }
    }        
    
    private void processJoin(int sourceNode) {
        println("Processing Spread JOIN_REQ Message from Node " + sourceNode);
        registeredNodes[sourceNode] = 1;
        sendToAll(new SpreadMessage(SpreadMessage.JOIN_OK, sourceNode));
    }
      
    
    private void sendToAll(Message msg) {
        /* send to  all, including requester */
        for(int i=1; i<=SlotsDonation.NODES; i++){
             out(i).send(msg);
        } 
        broadcasted++;        
    }
    
    public void broadCastMessage() {
        int index = in().select();
        Message msg = in(index).receive();

    }
    
    @Override
    public String getText() {
        return "Broadcasted " + broadcasted + " messages so far ";
    }
    
}