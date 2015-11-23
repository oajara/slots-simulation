import daj.Message;
import daj.Program;

import java.util.*;


public class ProgNormalNode extends Program {
    public static final int STS_ACTIVE = 1;
    public static final int STS_RUNNING = 2;
    public static final int STS_WAIT_STATUS = 3;
    public static final int STS_WAIT_INIT = 4;
    public static final int STS_JUST_BORN = 5;
    public static final int STS_REQ_SLOTS = 5;
    
    public static final int MIN_OWNED_SLOTS = 5;
    public static final int FREE_SLOTS_LOW = 2;
    public static final int SLOTS_BY_MSG = 5;
    
    private Random random;
    private Slot[] slotsTable = new Slot[SlotsDonation.TOTAL_SLOTS];
    private int[] registeredNodes = new int[SlotsDonation.MAX_NODES+1];
    private boolean[] initializedNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private int nodeId;
    private int state = STS_JUST_BORN;
    private int activeNodes = 0;
    private int primaryMember;
    
    public ProgNormalNode(int id) { 
        this.random = new Random();
        this.nodeId = id;
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            slotsTable[i] = new Slot(0, Slot.STATUS_FREE);
        }  
}
    
    private void println(String str) {
        System.out.println("Node[" + nodeId + "]: "+str);
    }    
    
    @Override
    public void main()  {
        println("Initializing (sending JOIN message)");
        
        // Join Spread
        out(0).send(new SpreadMessageJoin(this.nodeId));
        
        // Start with algorithm
        slotsLoop();
    }
    
    private void slotsLoop() {
        while(true) {
            println("Waiting for message...");
            int index = in().select();
            Message msg = in(index).receive();

            if (!(msg instanceof SpreadMessage)) {
                println("Received Slots Message");
                if(msg instanceof SlotsMessageRequest) {
                    handleSlotsRequest((SlotsMessageRequest)msg);
                } else if (msg instanceof SlotsMessageDonate) {
                    handleSlotsDonation((SlotsMessageDonate)msg);
//                } else if (msg instanceof SlotsMessagePutStatus) {
//                    handleSlotsPutStatus((SlotsMessagePutStatus)msg);
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
    }
    
    private int getActiveNodes() {
        int counter = 0;
        for(int i = 0; i < SlotsDonation.MAX_NODES; i++) {
            if(registeredNodes[i] == 1) {
                counter++;
            }
        }        
        return counter;        
    }
    
    private int getInitializedNodes() {
        int counter = 0;
        for(int i = 1; i <= SlotsDonation.MAX_NODES; i++) {
            if(this.initializedNodes[i]) {
                counter++;
            }
        }        
        return counter;        
    }    
    
    private void handleSpreadJoin(SpreadMessageJoin msg) {
        println("Handling Spread Join");
        /* update registered nodes table */
        this.registeredNodes = msg.getRegisteredNodes();        
        
        if( msg.getSenderId() == this.nodeId){ /* The own JOIN message	*/
            println("Received my own Join message");

            if (getActiveNodes() == 1) { 		/* It is a LONELY member*/
                println("I'm the first active node");
                
                /* it is ready to start running */
                this.state = STS_RUNNING;
                this.initializedNodes[nodeId] = true;

                /* it is the Primary Member 	*/
                this.primaryMember = nodeId;

                /* allocate all slots to the member */
                for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
                    slotsTable[i].setOwner(nodeId);
                }   
                
            } else {
                /* Waiting Global status info */
                
                this.state = STS_WAIT_STATUS;	
                println("New Status: "+this.getStateAsString());
            }
	} else { /* Other node JOINs the group	*/
            println("Other member joines the group:"+nodeId+". My State="+this.getStateAsString());

            /* I am not initialized yet */
            if( (!this.initializedNodes[nodeId]) && (this.state != STS_WAIT_INIT)) {
                println("I'm not Inizialized nor waiting init. My State:" +this.getStateAsString());
                return;
            }

            if (primaryMember == nodeId && this.state == STS_RUNNING ) { 	
                sendStatusInfo();
            }
	}
    }
    
    private void sendStatusInfo() {
        SlotsMessagePutStatus msg = new SlotsMessagePutStatus(
                this.slotsTable, this.initializedNodes, this.nodeId);

        /* Send the Global status info to new members */
        println("Send Global status");
        broadcast(msg);
    }    
    
    private void handleSpreadLeave(SpreadMessageLeave msg) {
        println("Handling Spread Leave");
    }    

        
    /*---------------------------------------------------------------------
    *			sp_req_slots
    *   SYS_REQ_SLOTS: A Systask on other node has requested free slots
    *---------------------------------------------------------------------*/
    private void handleSlotsRequest(SlotsMessageRequest msg) {
        int donated_slots, don_nodes, surplus;
        int requester = msg.getSenderId();
        
        println("Handling slot request from Node#"+requester);

        /* Ignore owns requests  */
        if(requester == this.nodeId) {
            println("Won't donate to myself. Ignoring...");
            return;
        }
        
        /* the member is  not initialized yet, then it can't respond to this request */
        if( !(isInitialized(this.nodeId))) {
            println("Cannot donate, I am not initialized");
            return;
        }

        /* Verify if the requester is initialized */
        if( !(isInitialized(requester))) {
           println("ERROR: member "+requester+ " was not initilized. Initialized nodes: "+Arrays.toString(this.initializedNodes));
           return;
        }

        /*
        * ALL other initialized members respond to a request slot message 
        * but only members with enough slots will donate
        */
        if( this.state != STS_RUNNING) {
            donated_slots = 0;
        } else {
            don_nodes = getInitializedNodes() - 1;
            assert( don_nodes > 0);

            surplus = (getFreeSlots() - FREE_SLOTS_LOW);
//            println("free_slots=%d free_slots_low=%d needed_slots=%d don_nodes=%d surplus=%d\n"
//                    ,free_slots,free_slots_low,needed_slots,don_nodes, surplus);
            if( surplus > (msg.getNeedSlots()/don_nodes) ) {  /* donate the slots requested  */
                donated_slots = (msg.getNeedSlots()/don_nodes);
            } else if( surplus > (FREE_SLOTS_LOW/don_nodes) ) {
                /* donate slots at least up to complete the minimal number of free slots */
                donated_slots = (FREE_SLOTS_LOW/don_nodes);
            } else if( surplus > 0 ){ /* donate at least one */
                donated_slots = 1;
            } else {
                donated_slots = 0;
            }
        }

        /* Maximun number of slots that can be donated by message round - Limited by the message structure */
        if(donated_slots > SLOTS_BY_MSG) {
            donated_slots = SLOTS_BY_MSG;
        }
        println("Donated_slots="+donated_slots+" requester="+requester);
        
 
        /* build the list of donated slots */
        int[] donatedSlotsList = new int[donated_slots];
        int j = 0;
        if( donated_slots > 0) {
            /* Search free owned slots */
            for(int i = 0; (i <  SlotsDonation.TOTAL_SLOTS) && (donated_slots > 0) ;i++) {
                if( (slotsTable[i].isFree()) && (slotsTable[i].getOwner() == this.nodeId)) {
                    donated_slots--;
                    slotsTable[i].setOwner(requester);
                    slotsTable[i].setStatus(Slot.STATUS_DONATING);
                    donatedSlotsList[j] = i;
                    j++;
                }
            }
        }

        SlotsMessageDonate donMsg = new SlotsMessageDonate(
                msg.getSenderId(), donatedSlotsList, this.nodeId);        
        broadcast(donMsg);        
    }
    
    private void handleSlotsDonation(SlotsMessageDonate msg) {
        println("Handling Slots Donate from Node#"+msg.getSenderId());
        
        if(!(isInitialized(this.nodeId)) && this.state != STS_WAIT_INIT) {
            println("I am not initialized nor waiting initialization. Return.");
            return;
        }

	println("Donation of "+msg.getDonatedIdList().length+" slots from "+msg.getSenderId()
                +" to "+msg.getRequester());

	/* Is the destination an initialized member ? */
        if(!isInitialized(msg.getRequester())) {
            println("WARNING Destination member node#"+msg.getRequester()+" is not initialized");
            return;
	}

	/* Is the donor an initialized member ? */
        if(!isInitialized(msg.getSenderId())) {
            println("WARNING Source member node#"+msg.getSenderId()+" is not initialized");
            return;
	}        

	/* Change the owner of the donated slots */
        int[] donatedList = msg.getDonatedIdList();
	for( int j = 0; j < donatedList.length; j++) {
		int slotId = donatedList[j];
		this.slotsTable[slotId].setOwner(msg.getRequester());
	}

	/* If the slots are for other node, returns */
        if(msg.getRequester() != this.nodeId){
            return;
        }

        /* this is for me */
	if( this.state == STS_REQ_SLOTS && msg.getDonatedIdList().length > 0){
            this.state = STS_RUNNING;
	}	

//	CLR_BIT(bm_donors, spin_ptr->m_source);
	println("free_slots="+getFreeSlots()+" free_slots_low="+FREE_SLOTS_LOW);
	println("owned_slots="+getOwnedSlots()+" max_owned_slots=? bm_donors=?");        
    }
    
    private void handleSlotsPutStatus(SlotsMessagePutStatus msg) {
        println("Handling Slots Put Status");
        
        if( this.state != STS_WAIT_STATUS) {
            println("I am not waiting status info. Ignore...");
            return;
        }

        //println("SYS_PUT_STATUS: primarymember="+primaryMember+" table has %d slots");// ret/sizeof(slot_t));
        this.slotsTable = msg.getSlotsTable();
        println("Updating Initialized nodes table...");
        this.initializedNodes = msg.getInitializedNodes();
        this.state = STS_WAIT_INIT;
        
	/* Report to other nodes as INITILIZED */
	println("Multicasting SYS_INITIALIZED");
	broadcast(new SlotsMessageInitialized(this.nodeId));        
 
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
    
    /*--------------------------------------------------------------------
    *				handleSlotsInitialized
    *   SYS_INITIALIZED: A initialized member message has been received
    *  All members update their states
    *---------------------------------------------------------------------*/    
    private void handleSlotsInitialized(SlotsMessageInitialized msg) {
        println("Handling Slots Initialize. Init Member: " + msg.getSenderId());
        
        if(!(isInitialized(this.nodeId)) && (this.state != STS_WAIT_INIT)) {
            println("I am not initialized and not waiting initialization. Ignore...");
            return;
        }
        
        if(!isInitialized(msg.getSenderId())) {
            println("Marking Node#"+msg.getSenderId()+" as initialized in my table.");
            this.initializedNodes[msg.getSenderId()] = true;
        } else {
            println("WARNING member "+msg.getSenderId()+" just was initilized");
        }
        
        if(msg.getSenderId() == this.nodeId) {
            if(this.state == STS_WAIT_INIT) {
                println("I was waiting initialize msg");	
            }
            /* The member is initialized but it hasn't got slots to start running */
            this.state = STS_REQ_SLOTS;
            mbrRqstSlots(MIN_OWNED_SLOTS);
        } else {
            println("This was a initialization message of someone else. Ignore...");
            /* get remote SYSTASK  process descriptor pointer */
            /* is remote SYSTASK bound to the correct node ?*/
            /* Is the remote SYSTASK bound ? */
        }
        
        
    } 
    
    private int getFreeSlots() {
        int counter = 0;
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            if(slotsTable[i].isFree() && slotsTable[i].getOwner() == this.nodeId) {
                counter++;
            }
        }         
        return counter;
    }
    
    private int getUsedSlots() {
        int counter = 0;
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            if(slotsTable[i].isUsed() && slotsTable[i].getOwner() == this.nodeId) {
                counter++;
            }
        }         
        return counter;
    }   
    
    private int getOwnedSlots() {
        int counter = 0;
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            if(slotsTable[i].getOwner() == this.nodeId) {
                counter++;
            }
        }         
        return counter;
    }       
    
    private int countActive(boolean[] list) {
        int counter = 0;
        for(int i = 0; i < list.length; i++) {
            if(list[i]) {
                counter++;
            }
        }         
        return counter;
        
    } 
    
    /*===========================================================================*
     *				mbr_rqst_slots					   
     * It builds and broadcasts a message requesting slots
     *===========================================================================*/
    private void mbrRqstSlots(int nr_slots) {
        boolean[] bm_donors;
        
        println("Sending slot request");	

        /* set donors*/
        bm_donors = this.initializedNodes;
        bm_donors[this.nodeId] = false;

        if(countActive(bm_donors) == 0) {
            return;
        }
        
        SlotsMessageRequest msg = new SlotsMessageRequest(nr_slots,
                getFreeSlots(), getOwnedSlots(), this.nodeId);

        broadcast(msg);
    } 
    
    private int getPrimaryMember() {
        for(int i = 1; i <= SlotsDonation.MAX_NODES; i++) {
            if(registeredNodes[i] == 1) {
                return i;
            }
        }        
        return 0;
    }

        
   /**
     * Broadcasting a message is sending it to the spread node
     * @param msg 
     */
    public void broadcast(Message msg) {
        out(0).send(msg);
    }
    
    @Override
    public String getText() {
        return "Node #" + nodeId + "\nStatus: "+ getStateAsString() 
                +"\nI own: "+ getSlotsNumber() + " slots\nRegistered Nodes: "
                + Arrays.toString(this.registeredNodes)+"\nInitialized Nodes: "
                + Arrays.toString(this.initializedNodes);
    }
    
    public int getSlotsNumber() {
        int counter = 0;
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            if(slotsTable[i].getOwner() == this.nodeId) {
                counter++;
            }
        }        
        return counter;
    }
    
    private String getStateAsString() {
        switch(this.state) {
            case STS_JUST_BORN:
                return "Just Born";            
            case STS_ACTIVE:
                return "Active";
            case STS_RUNNING:
                return "Running";
            case STS_WAIT_STATUS:
                return "Wait Status";
            case STS_WAIT_INIT:
                return "Wait Init";                
            default:
                return "Unknown Status: "+this.state;
        }
    }

}