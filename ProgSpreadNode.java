import daj.Message;
import daj.Program;
import java.util.Arrays;

/**
 * Program for each node.
 */

public class ProgSpreadNode extends Program {
    int broadcasted = 0;
    private final boolean[] registeredNodes = new boolean[SlotsDonation.MAX_NODES+1];
    
    public ProgSpreadNode() {
        /* no nodes at the beggining */
        for (int i = 0; i <= SlotsDonation.MAX_NODES; i++ ) {
            registeredNodes[i] = false;
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
        return registeredNodes[nodeId] == true;
    }
    
    private void processJoin(SpreadMessageJoin msg) { 
        println("Processing Spread JOIN Message from Node " + msg.getSenderId());
        if(this.isActive(msg.getSenderId())) {
            println("You are already joined, Node " + msg.getSenderId());
            return;
        }        
        this.registeredNodes[msg.getSenderId()] = true;
        msg.setActiveNodes(cloneBitmapTable(this.registeredNodes));
        sendToAll(msg);
    }
    
    private void processLeave(SpreadMessageLeave msg) { 
        println("Processing Spread LEAVE Message from Node " + msg.getSenderId());
        if(!this.isActive(msg.getSenderId())) {
            println("You are not joined, Node " + msg.getSenderId());
            return;
        }          
        this.registeredNodes[msg.getSenderId()] = false;
        msg.setRegisteredNodes(cloneBitmapTable(this.registeredNodes));
        sendToAll(msg);
    } 
    
    private boolean[] cloneBitmapTable(boolean[] table) {
        boolean[] cloneTable;
        cloneTable = Arrays.copyOf(table, SlotsDonation.MAX_NODES+1);
        return cloneTable;       
    }
      
    
    private void sendToAll(Message msg) {
        /* send to  all active nodes, including requester */
        println("Sending message to all active nodes...");
        for(int i=1; i<SlotsDonation.NODES+1; i++){
            if(this.isActive(i)) {
                this.println("... to node#"+i);
                out(i-1).send(msg); // because link to node n is locate at out(n-1)
            }
        } 
        broadcasted++;        
    }
    
    public String getText() {
        int count = 0;
        for (int i = 1; i <= SlotsDonation.MAX_NODES; i++ ) {
            if (this.isActive(i)) {
                count ++;
            }
            
        }
        return "Active Nodes: " + count+ ".Broadcasted " + broadcasted + " messages so far ";
    }
    
}