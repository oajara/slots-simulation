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
        } else {
            handleSlotsMessage((SlotsMessage)msg);
        }
    }
    
    private void handleSpreadMessage(SpreadMessage msg) {
        if (msg instanceof SpreadMessageJoin) {
            processJoin((SpreadMessageJoin)msg);
        } else if (msg instanceof SpreadMessageLeave) {
            processLeave((SpreadMessageLeave)msg);
        }
    }        
    
    private void handleSlotsMessage(SlotsMessage msg) {
        if(isActive(msg.getSenderId())) {
            sendToAll(msg);
        } else {
            println("Received a Slot message from a non-active node. Discard...");
        }
    }        
    
    private boolean isActive(int nodeId) {
        return registeredNodes[nodeId] == 1;
    }
    
    private void processJoin(SpreadMessageJoin msg) { 
        println("Processing Spread JOIN Message from Node " + msg.getSenderId());
        registeredNodes[msg.getSenderId()] = 1;
        msg.setRegisteredNodes(registeredNodes);
        sendToAll(msg);
    }
    
    private void processLeave(SpreadMessageLeave msg) { 
        println("Processing Spread LEAVE Message from Node " + msg.getSenderId());
        registeredNodes[msg.getSenderId()] = 0;
        msg.setRegisteredNodes(registeredNodes);
        sendToAll(msg);
    } 
      
    
    private void sendToAll(Message msg) {
        /* send to  all, including requester */
        println("Sending message to all nodes...");
        for(int i=0; i<SlotsDonation.NODES; i++){
            
            out(i).send(msg);
        } 
        broadcasted++;        
    }
    
    public String getText() {
        int count = 0;
        for (int i = 1; i <= SlotsDonation.MAX_NODES; i++ ) {
            if (registeredNodes[i] == 1) {
                count ++;
            }
            
        }
        return "Active Nodes: " + count+ ".Broadcasted " + broadcasted + " messages so far ";
    }
    
}