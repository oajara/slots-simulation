import daj.Message;
import daj.Program;

import java.util.*;


public class ProgNormalNode extends Program {
    public static final int INACTIVE = 0;
    public static final int ACTIVE = 1;
    public static final int READY = 2;
        
    private Random random;
    private int[] slotsTable = new int[SlotsDonation.SLOTS];
    private int[] registeredNodes = new int[SlotsDonation.MAX_NODES+1];
    private int nodeId;
    private int status;
    
    public ProgNormalNode(int id, int slots) { 
        this.random = new Random();
        this.status = INACTIVE;
        this.nodeId = id;
        
        /* initialize slots table */
        for (int i = 1; i < SlotsDonation.SLOTS; i++ ) {
            slotsTable[i] = 0;
        }        
    }
    
    private void println(String str) {
        System.out.println("Node[" + nodeId + "]: "+str);
    }    
    
    public void main()  {
        println("Initializing...");
        // Join Spread
        out(0).send(new SpreadMessage(SpreadMessage.JOIN_REQ, nodeId));
        Message msg;
        while(status == INACTIVE) {
            println("Waiting for join ok message...");
            msg = in(0).receive();
            if (msg instanceof SpreadMessage && ((SpreadMessage)msg).getNodeId() == nodeId) {
                println("Joined!");
                status = ACTIVE;
                registeredNodes = ((SpreadMessage)msg).getData();
            } else {
                println("Received a message but not what I am waiting for. Ignore.");
            }
        }
        
        /* now I need a valid slots table */
        if(iAmPrimary()) {
            /* all slots are mine */
            for(int i = 0; i<SlotsDonation.SLOTS; i++) {
                slotsTable[i] = nodeId;
                status = READY;
            }
        } else {
            /* wait for sync info */
            while(status == ACTIVE) {
                println("Waiting for join ok message...");
                msg = in(0).receive();
                if (msg instanceof SpreadMessage && ((SpreadMessage)msg).getNodeId() == nodeId) {
                    println("Ready!");
                    status = READY;
                    slotsTable = ((SlotsMessageSync)msg).getTable();
                } else {
                    println("Received a message but not what I am waiting for. Ignore.");
                }
            }            
        }
        
        /* Now I am ready to go */
        startDonatingAlgorithm();
           
        /* I am done. Leave */
        out(0).send(new SpreadMessage(SpreadMessage.LEAVE_REQ, nodeId));
    }
    
    private boolean iAmPrimary() {
        return nodeId == this.primaryNodeId();
    }
    
    /** Primary node is the registered node with lower ID **/
    private int primaryNodeId() {
        for (int i = 1; i <= SlotsDonation.MAX_NODES; i++ ) {
            if(registeredNodes[i] == 1) {
                return i;
            }
        }
        return 0;
    }
    
    private void startDonatingAlgorithm() {
        println("This should run the rest of the algorithm.");
        while(true)
            receiveFromAll();        
    }    
    
    private void receiveFromAll() {
        println("Waiting for message...");
        int index = in().select();
        Message msg = in(index).receive();
        
        if (msg instanceof SpreadMessage) {
            println("Received Spread Message");
            handleSpreadMessage((SpreadMessage)msg);
        } else if (msg instanceof SlotsMessageSync) {
            handleSlotsMessageSync((SlotsMessageSync)msg);
        } else if (msg instanceof SlotsMessageDonate) {
            handleSlotsMessageDonate((SlotsMessageDonate)msg);
        } else if (msg instanceof SlotsMessageRequest) {
            handleSlotsMessageRequest((SlotsMessageRequest)msg);
        }
    }    

    private void handleSlotsMessageRequest(SlotsMessageRequest msg) {
        println("Handling Slots Request");
    }
    
    private void handleSlotsMessageDonate(SlotsMessageDonate msg) {
        println("Handling Slots Donate");
    }
    
    private void handleSlotsMessageSync(SlotsMessageSync msg) {
        println("Handling Slots Sync");
    }    
    
    private void handleSpreadMessage(SpreadMessage msg) {
        println("Handling Spread Message");
    }        
    
    /**
     * Broadcasting a message is sending it to the spread node
     * @param msg 
     */
    public void broadcast(Message msg) {
        out(0).send(msg);
    }
    
    public String getText() {
        return "Node: " + nodeId + "\nStatus: "+ getStatusAsString() +"\nI own: "+ getSlotsNumber() + " slots";
    }
    
    public int getSlotsNumber() {
        int counter = 0;
        for(int i = 0; i<SlotsDonation.SLOTS; i++) {
            if(slotsTable[i] == nodeId) {
                counter++;
            }
        }        
        return counter;
    }
    
    private String getStatusAsString() {
        switch(this.status) {
            case INACTIVE:
                return "Inactive";       
            case ACTIVE:
                return "Active";                
            case READY:
                return "Ready";       
            default:
                return "Unknown";
        }
    }
}