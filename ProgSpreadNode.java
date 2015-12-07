import daj.Message;
import daj.Program;
import java.util.Arrays;

/**
 * Program for each node.
 */

public class ProgSpreadNode extends Program {
    int broadcasted = 0;
    private final boolean[] registeredNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private final MessageCounter[] counters = new MessageCounter[SlotsDonation.MAX_NODES+1];
    
    public ProgSpreadNode() {
        /* no nodes at the beggining */
        for (int i = 0; i <= SlotsDonation.MAX_NODES; i++ ) {
            registeredNodes[i] = false;
            counters[i] = new MessageCounter();
            counters[i].reset();
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
            println("Receive "+ msg.getClass().toString() +" from Node#"+ ((SpreadMessage)msg).getSenderId());
            handleSpreadMessage((SpreadMessage)msg);
        } else {
            println("Receive "+ msg.getClass().toString() +" from Node#"+ ((SlotsMessage)msg).getSenderId());
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
            this.registerCount(msg);
            sendToAll(msg);
        } else {
            println("Received a Slot message from a non-active node. Discard...");
        }
    }        
    
    private void registerCount(SlotsMessage msg) {
        int sid = msg.getSenderId();
        if(msg instanceof SlotsMessageRequest) {
            this.counters[sid].inc(MessageCounter.INDEX_REQUEST);
        } else if (msg instanceof SlotsMessageDonate) {
            this.counters[sid].inc(MessageCounter.INDEX_DONATE);
        } else if (msg instanceof SlotsMessagePutStatus) {
            this.counters[sid].inc(MessageCounter.INDEX_PUTSTATUS);
        }  else if (msg instanceof SlotsMessageInitialized) {
            this.counters[sid].inc(MessageCounter.INDEX_INITIALIZED);
        } else if (msg instanceof SlotsMessageNewStatus) {
            this.counters[sid].inc(MessageCounter.INDEX_NEWSTATUS);
        } else if (msg instanceof SlotsMessageMergeStatus) {
            this.counters[sid].inc(MessageCounter.INDEX_MERGESTATUS);
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
        this.counters[msg.getSenderId()].inc(MessageCounter.INDEX_JOIN);
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
        this.counters[msg.getSenderId()].inc(MessageCounter.INDEX_LEAVE);
        sendToAll(msg);
    } 
    
    private boolean[] cloneBitmapTable(boolean[] table) {
        boolean[] cloneTable;
        cloneTable = Arrays.copyOf(table, SlotsDonation.MAX_NODES+1);
        return cloneTable;       
    }
      
    
    private void sendToAll(Message msg) {
        /* send to  all active nodes, including requester */
        //println("Sending message to all active nodes...");
        for(int i=1; i<SlotsDonation.NODES+1; i++){
            if(this.isActive(i)) {
                //this.println("... to node#"+i);
                out(i-1).send(msg); // because link to node n is locate at out(n-1)
            }
        } 
        broadcasted++;        
    }
    
    public void getInfoLine() {
        //(NodeId, Forks OK, Forks Failed, Exits)
        //System.out.println("0,"+this.
        int counters[] = new int[8];
        for(int j = 0; j <= 7; j++) { //type of message
            counters[j] = 0;
            for (int i = 1; i <= SlotsDonation.MAX_NODES; i++ ) { //node#
                counters[j] = this.counters[i].get(j) + counters[j];
            }
        }
//        INDEX_JOIN = 0;
//        INDEX_LEAVE = 1;
//        INDEX_REQUEST = 2;
//        INDEX_DONATE = 3;
//        INDEX_INITIALIZED = 4;
//        INDEX_PUTSTATUS = 5;
//        INDEX_NEWSTATUS = 6;
//        INDEX_MERGESTATUS = 7;
        System.out.println(""+counters[MessageCounter.INDEX_JOIN]+","+counters[MessageCounter.INDEX_LEAVE]+","
        +counters[MessageCounter.INDEX_REQUEST]+","+counters[MessageCounter.INDEX_DONATE]+","
        +counters[MessageCounter.INDEX_INITIALIZED]+","+counters[MessageCounter.INDEX_PUTSTATUS]+","
        +counters[MessageCounter.INDEX_NEWSTATUS]+","+counters[MessageCounter.INDEX_MERGESTATUS]);
    }    
    
    public String getText() {
        int count = 0;
        String msgs = new String();
        for (int i = 1; i <= SlotsDonation.MAX_NODES; i++ ) {
            if (this.isActive(i)) {
                count ++;
            }
            msgs = msgs + "\nNode #" + i + ": " + this.counters[i].asString();
        }
        return "Active Nodes: " + count+ "\nMessages: "+msgs;
    }
    
}