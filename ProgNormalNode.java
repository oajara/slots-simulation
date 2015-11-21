import daj.Message;
import daj.Program;

import java.util.*;


public class ProgNormalNode extends Program {
    public static final int STS_ACTIVE = 1;
    public static final int STS_RUNNING = 2;
    public static final int STS_WAIT_STATUS = 3;
    public static final int STS_WAIT_INIT = 4;
        
    private Random random;
    private Slot[] slotsTable = new Slot[SlotsDonation.SLOTS];
    private int[] registeredNodes = new int[SlotsDonation.MAX_NODES+1];
    private boolean[] initializedNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private int nodeId;
    private int state;
    private int activeNodes = 0;
    private int primaryMember;
    private int ownedSlots;
    
    public ProgNormalNode(int id) { 
        this.random = new Random();
        this.nodeId = id;
}
    
    private void println(String str) {
        System.out.println("Node[" + nodeId + "]: "+str);
    }    
    
    public void main()  {
        println("Initializing...");
        // Join Spread
        out(0).send(new SpreadMessageJoin(this.nodeId));
 
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
            slotsLoop();        
    }    
    
    private void slotsLoop() {
        println("Waiting for message...");
        int index = in().select();
        Message msg = in(index).receive();
        
        if (!(msg instanceof SpreadMessage)) {
            println("Received Slots Message");
            if(msg instanceof SlotsMessageRequest) {
                handleSlotsRequest((SlotsMessageRequest)msg);
            } else if (msg instanceof SlotsMessageDonate) {
                handleSlotsDonation((SlotsMessageDonate)msg);
            } else if (msg instanceof SlotsMessagePutStatus) {
                handleSlotsPutStatus((SlotsMessagePutStatus)msg);
            }  else if (msg instanceof SlotsMessageInitialized) {
                handleSlotsInitialized((SlotsMessageInitialized)msg);
            } else if (msg instanceof SlotsMessageNewStatus) {
                handleSlotsNewStatus((SlotsMessageNewStatus)msg);
            } else if (msg instanceof SlotsMessageMergeStatus) {
                handleSlotsMergeStatus((SlotsMessageMergeStatus)msg);
            }
        } else {
            println("Received Spread Message");
            if(msg instanceof SpreadMessageJoin) {
                handleSpreadJoin((SpreadMessageJoin)msg);
            } else if (msg instanceof SpreadMessageLeave) {
                handleSpreadLeave((SpreadMessageLeave)msg);
            }
        }
    }    
    
    private void handleSpreadJoin(SpreadMessageJoin msg) {
        println("Handling Spread Join");
        if( msg.getNewNode() == this.nodeId){ /* The own JOIN message	*/
            if (activeNodes == 1) { 		/* It is a LONELY member*/

                /* it is ready to start running */
                this.state = STS_RUNNING;

                /* it is the Primary Member 	*/
                this.primaryMember = nodeId;

                /* update registered nodes table */
                this.registeredNodes = msg.getRegisteredNodes();
                
                /* allocate all slots to the member */
                this.ownedSlots = SlotsDonation.TOTAL_SLOTS;		
                for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
                    slotsTable[i].setOwner(nodeId);
                }   
                
            } else {
                /* Waiting Global status info */
                this.state = STS_WAIT_STATUS;	
            }
	} else { /* Other node JOINs the group	*/
            println("Member "+nodeId+" state="+this.state);

            /* I am not initialized yet */
            if( (this.initializedNodes[nodeId]) && (this.state != STS_WAIT_INIT)) {
                return;
            }

            /* if the new member was previously considered as a member of other    */
            /* partition but really had crashed, allocate its slots to primary_mbr */
            println("TODO: Is the new member previously considered as a member of other partition? new_mbr="+msg.getNewNode());
//		for (i = vm_ptr->vm_nr_sysprocs; i < (vm_ptr->vm_nr_tasks + vm_ptr->vm_nr_procs); i++) {
//                    if ( (slot[i].s_owner == new_mbr)
//                        && 	(TEST_BIT(slot[i].s_flags, BIT_SLOT_PARTITION) )) {
//                        if (primary_mbr == local_nodeid){
//                            free_slots++;
//                            owned_slots++;
//                        }
//                        total_slots++;
//                        slot[i].s_owner = primary_mbr;
//                        CLR_BIT(slot[i].s_flags, BIT_SLOT_PARTITION);
//                        TASKDEBUG("Restoring slot %d from member %d considered alive in other partition\n",
//                                i, new_mbr);
//                    }
//		}

            if (primaryMember == nodeId && this.state == STS_RUNNING ) { 	
                sendStatusInfo();
            }
	}
    }
    
    private void sendStatusInfo() {
        SlotsMessagePutStatus msg = new SlotsMessagePutStatus(
                this.slotsTable, this.initializedNodes, this.nodeId);

        /* Send the Global status info to new members */
        println("Send Global status\n");
        broadcast(msg);
    }    
    
    private void handleSpreadLeave(SpreadMessageLeave msg) {
        println("Handling Spread Leave");
    }    

    private void handleSlotsRequest(SlotsMessageRequest msg) {
        println("Handling Slots Request");
    }
    
    private void handleSlotsDonation(SlotsMessageDonate msg) {
        println("Handling Slots Donate");
    }
    
    private void handleSlotsPutStatus(SlotsMessagePutStatus msg) {
        println("Handling Slots Put Status");
        int member =msg.getSenderId();
        
        //if( !(FSM_state & MASK_INITIALIZED)) {
        if( !isInitialized(this.nodeId)) {
            primaryMember = member;
        } else {
            if(primaryMember != member) {
                println("SYS_PUT_STATUS: current primaryMember="+primaryMember+" differs from new primarymember="+member);
            }
            if(isInitialized(member)){
                println("SYS_PUT_STATUS: primaryMember="+primaryMember+" is not in bm_init");
            }
        }
        //println("SYS_PUT_STATUS: primarymember="+primaryMember+" table has %d slots");// ret/sizeof(slot_t));
        this.slotsTable = msg.getSlotsTable();
        this.initializedNodes = msg.getInitializedNodes();
        this.state = STS_WAIT_INIT;
        
	/* Report to other nodes as INITILIZED */
	println("Multicasting SYS_INITIALIZED");

        SlotsMessageInitialized new_msg = new SlotsMessageInitialized(this.nodeId);
	broadcast(new_msg);        
    }
    
    private boolean isInitialized(int nodeId) {
        return this.initializedNodes[nodeId];
    }
    
    private void handleSlotsNewStatus(SlotsMessageNewStatus msg) {
        println("Handling Slots New Status");
    }    
    
    private void handleSlotsMergeStatus(SlotsMessageMergeStatus msg) {
        println("Handling Slots Merge Status");
    }        
    
    private void handleSlotsInitialized(SlotsMessageInitialized msg) {
        println("Handling Slots Initialize");
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
            if(slotsTable[i].getOwner() == nodeId) {
                counter++;
            }
        }        
        return counter;
    }
    
    private String getStatusAsString() {
        switch(this.status) {
            case STS_ACTIVE:
                return "Inactive";       
            case STS_ACTIVE:
                return "Active";                
            case STS_RUNNING:
                return "Ready";       
            default:
                return "Unknown";
        }
    }

}