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
        while(true)
            receiveFromAll();
    }
    
    private void println(String str) {
        System.out.println("Node[SP]: "+str);
    }
    
    private void receiveFromAll() {
        println("Waiting for message...");
        int index = in().select();
        Message msg = in(index).receive();
        
        if (msg instanceof SpreadMessage) {
            println("Received Spread Message");
            handleSpreadMessage((SpreadMessage)msg);
        } else if ((msg instanceof SlotsMessageSync) ||
         (msg instanceof SlotsMessageRequest)
                || (msg instanceof SlotsMessageDonate)){
            handleSlotsMessage((SlotsMessage)msg);
        }
    }
    
    private void handleSpreadMessage(SpreadMessage msg) {
        switch(msg.getType()) {
            case SpreadMessage.JOIN_REQ:
                processJoin(msg.getNodeId());
                break;
            case SpreadMessage.LEAVE_REQ:
                processLeave(msg.getNodeId());
                break;
        }
    }        
    
    private void handleSlotsMessage(SlotsMessage msg) {
        println("This show handle a slot message!");
    }        
    
    private void processJoin(int sourceNode) {
        println("Processing Spread JOIN_REQ Message from Node " + sourceNode);
        registeredNodes[sourceNode] = 1;
        sendToAll(new SpreadMessage(SpreadMessage.JOIN_OK, sourceNode, this.registeredNodes));
    }
    
    private void processLeave(int sourceNode) {
        println("Processing Spread LEAVE_REQ Message from Node " + sourceNode);
        registeredNodes[sourceNode] = 0;
        sendToAll(new SpreadMessage(SpreadMessage.LEAVE_OK, sourceNode, this.registeredNodes));
    }    
      
    
    private void sendToAll(Message msg) {
        /* send to  all, including requester */
        for(int i=0; i<SlotsDonation.NODES; i++){
            println("Sending message to " + i);
            out(i).send(msg);
        } 
        broadcasted++;        
    }
    
    public void broadCastMessage() {
        int index = in().select();
        Message msg = in(index).receive();

    }
    
    public String getText() {
        int count = 0;
        for (int i = 1; i <= SlotsDonation.MAX_NODES; i++ ) {
            if (registeredNodes[i] == 1) {
                count ++;
            }
            
        }
        return "Nodes Joined: " + count+ ".Broadcasted " + broadcasted + " messages so far ";
    }
    
}