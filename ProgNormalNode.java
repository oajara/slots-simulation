import daj.Message;
import daj.Program;
import java.util.*;

public class ProgNormalNode extends Program {
    public static final int STS_ACTIVE = 1;
    public static final int STS_RUNNING = 2;
    public static final int STS_WAIT_STATUS = 3;
    public static final int STS_WAIT_INIT = 4;
    public static final int STS_DISCONNECTED = 5;
    public static final int STS_REQ_SLOTS = 6;
    public static final int STS_NEW = 7;
    public static final int STS_MERGE_STATUS = 8;
    
    public static final int NO_PRIMARY_MBR = -1;
    
    public static final int MIN_OWNED_SLOTS = 2;
    public static final int FREE_SLOTS_LOW = 5;
    public static final int SLOTS_BY_MSG = 5;
    
    private Random random;
    private Slot[] slotsTable = new Slot[SlotsDonation.TOTAL_SLOTS];
    private boolean[] registeredNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private boolean[] initializedNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private boolean[] donorsNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private int nodeId;
    private int state = STS_DISCONNECTED;
    private int primaryMember;
    private int maxOwnedSlots;
    
    public ProgNormalNode(int id) { 
        this.random = new Random();
        this.nodeId = id;
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            slotsTable[i] = new Slot(0, Slot.STATUS_FREE);
        }  
}
    
    public void println(String str) {
        System.out.println("Node[" + nodeId + "]: "+str);
    }    
    
    @Override
    public void main()  {
        this.println("Initializing (sending JOIN message)");

        // Join Spread
        this.connect();
        
        // Start with algorithm
        this.slotsLoop();
    }
    
    public boolean doFork() {
        int leftover;
        this.println("Executing a fork");
        
	if( this.getFreeSlots() < FREE_SLOTS_LOW ) {
            this.println("I'm below FREE_SLOTS_LOW");
            if( this.getOwnedSlots() < this.maxOwnedSlots ) { /* do I achieve the maximum threshold  ? */
                if(this.countActive(this.donorsNodes) == 0) { 	/*  if there pending donation requests? */
                    if( this.getOwnedSlots() < (this.maxOwnedSlots/this.getInitializedNodes()))
                        leftover = (this.maxOwnedSlots/getInitializedNodes()) - getOwnedSlots();
                    else
                        leftover = FREE_SLOTS_LOW - getFreeSlots();
                    this.println("leftover="+leftover+" free_slots="+
                            this.getFreeSlots()+" free_slots_low="+FREE_SLOTS_LOW+
                            " bm_donors="+Arrays.toString(this.donorsNodes));
                    this.println("owned_slots="+this.getOwnedSlots()+
                            " max_owned_slots="+this.maxOwnedSlots);
                    this.println("Requesting slot donation");
                    this.mbrRqstSlots(leftover);
                }
            }
        } 
	
	if(this.getFreeSlots() == 0) {
            this.println("No frees slot");
            return(false);
	} else {
            println("Free slot found, forking");
            this.markSlotUsed();
            return (true);
        }        
    }
    
    public void doExit() {
        println("Executing exit of a process");
        this.markSlotFree();
    }
    
    /** find first owned busy slot and free it **/
    private void markSlotFree() {
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            if(slotsTable[i].isUsed() && slotsTable[i].getOwner() == this.nodeId) {
                slotsTable[i].setStatus(Slot.STATUS_FREE);
                return;
            }
        }           
    }
    
    /** find first owned free slot and use it **/
    private void markSlotUsed() {
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            if(slotsTable[i].isFree() && slotsTable[i].getOwner() == this.nodeId) {
                slotsTable[i].setStatus(Slot.STATUS_USED);
                return;
            }
        }           
    }
    
    private void slotsLoop() {
        Message msg;
        while(true) {
            this.println("Waiting for message...");
            msg = this.in(0).receive(2);
            if (msg != null) {

                if (!(msg instanceof SpreadMessage)) {
                    this.println("Received Slots Message");
                    if(msg instanceof SlotsMessageRequest) {
                        this.handleSlotsRequest((SlotsMessageRequest)msg);
                    } else if (msg instanceof SlotsMessageDonate) {
                        this.handleSlotsDonation((SlotsMessageDonate)msg);
                    } else if (msg instanceof SlotsMessagePutStatus) {
                        this.handleSlotsPutStatus((SlotsMessagePutStatus)msg);
                    }  else if (msg instanceof SlotsMessageInitialized) {
                        this.handleSlotsInitialized((SlotsMessageInitialized)msg);
                    } else if (msg instanceof SlotsMessageNewStatus) {
                        this.handleSlotsNewStatus((SlotsMessageNewStatus)msg);
                    } else if (msg instanceof SlotsMessageMergeStatus) {
                        this.handleSlotsMergeStatus((SlotsMessageMergeStatus)msg);
                    }
                } else {
                    this.println("Received Spread Message");
                    if(msg instanceof SpreadMessageJoin) {
                        this.handleSpreadJoin((SpreadMessageJoin)msg);
                    } else if (msg instanceof SpreadMessageLeave) {
                        this.handleSpreadLeave((SpreadMessageLeave)msg);
                    }
                }
            }
            
            this.processStuff();
            
            
        }    
    }
    
    private void processStuff() {
        int action;
        for(int i = this.random.nextInt(4); i > 0; i--) {
            action = this.random.nextInt(6);
            switch(action) {
                case 0:
                    if(this.isInitialized()) {
                        this.doFork();
                    }
                    break;
                case 1:
                    if (this.isInitialized() && this.getUsedSlots() > 0) {
                        this.doExit();
                    }
                    break;
                case 2:
                    if (this.isConnected()) {
                        this.disconnect();
                    }
                    break;
                case 3:
                    if (!this.isConnected()) {
                        this.connect();
                    }                    
                    break;
                case 4:
                    break;
                default:
                    break;
            }
        }
    }
    
    private boolean isConnected() {
        return (this.registeredNodes[this.nodeId] == true);
    }
    
    private void connect() {
        out(0).send(new SpreadMessageJoin(this.nodeId));
    }
    
    private void disconnect() {
        out(0).send(new SpreadMessageLeave(this.nodeId));
    }    
    
    private int getActiveNodes() {
        int counter = 0;
        for(int i = 0; i < SlotsDonation.MAX_NODES; i++) {
            if(registeredNodes[i] == true) {
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
    
    private void initGlobalVars() {
	this.state = STS_NEW;	
	this.primaryMember = NO_PRIMARY_MBR;
        for(int i = 1; i <= SlotsDonation.MAX_NODES; i++) {
            this.initializedNodes[i] = false;
            this.registeredNodes[i] = false;
            this.donorsNodes[i] = false;
        }        
    }
    
    private void handleSpreadLeave(SpreadMessageLeave msg) {
        this.println("Handling Spread Leave");
        
        int disc_mbr = msg.getSenderId();
        
        this.registeredNodes = msg.getRegisteredNodes();
        if( disc_mbr == this.nodeId){ /* The own LEAVE message	*/
            this.println("Received my own Join message");
            this.state = STS_DISCONNECTED;
            return;
        }
        
	/* if local_nodeid is not initialized and is the only surviving node , restart all*/
	if(!this.isInitialized() && this.getActiveNodes() == 1) {
		this.initGlobalVars();
		this.disconnect();
		this.connect();
		return;
	}     
        
	/* I am not initialized yet */
	if( !this.isInitialized() && (this.state != STS_WAIT_INIT))  {
		return;        
        }
        
        /* mark node as not initialized */
        this.initializedNodes[disc_mbr] = false;
        
	/* verify if the local member is waiting a donation from the disconnected node */
	if(this.donorsNodes[disc_mbr]) {
            this.donorsNodes[disc_mbr] = false;
	}        
        
	/* if the dead node was the primary_mbr, search another primary_mbr */
	if( this.primaryMember == disc_mbr) {
            /* Is the process waiting STS_MERGE_STATUS message from the dead primary_mbr? */
            if(this.state == STS_MERGE_STATUS)
                    this.state = STS_RUNNING;
            this.primaryMember = this.getPrimaryMember();
            if( this.primaryMember == NO_PRIMARY_MBR) {
                    this.println("primary_mbr == NO_PRIMARY_MBR");
                    this.state = STS_NEW;
                    this.initGlobalVars();
                    this.disconnect();
                    this.connect();
                    return;
            }		
	}     
        
	/* recalculate global variables */
	this.maxOwnedSlots = (SlotsDonation.TOTAL_SLOTS -
                (MIN_OWNED_SLOTS*(this.countActive(this.initializedNodes)-1)));
	this.println("max_owned_slots="+this.maxOwnedSlots);        
        
	/* reallocate slots of the disconnecter member to the primary member */
	for(int i = 1; i <= SlotsDonation.MAX_NODES; i++) {
            if( this.slotsTable[i].getOwner() == disc_mbr ) {
                /* an uncompleted donation */
                if( this.slotsTable[i].getStatus() == Slot.STATUS_DONATING) { 
                    /* local node was the donor */
                    this.slotsTable[i].setOwner(this.nodeId);
                    this.slotsTable[i].setStatus(Slot.STATUS_FREE);
                } else {
                    this.slotsTable[i].setOwner(this.getPrimaryMember());
                    if( this.getPrimaryMember() == this.nodeId){
                        /* The local node is the new primary and 
                         * has inherit disconnected members slots
                         * if it has not any slot it is blocked waiting for them.
                         * therefore, now SYSTASK must be signaled 
                         */
                        if( (this.state == STS_REQ_SLOTS) &&
                                (this.getOwnedSlots() == 0)){
                                this.state = STS_RUNNING;
                        }
                    }
                }
            }
	}
        
	/*
	* the disconnected member may be the previous primary_mbr
	* if it leaves the group without finishing new nodes synchronization
	* the new primary_mbr must do that operations.
	*/
	if( this.nodeId == this.getPrimaryMember()){
            this.sendStatusInfo();
	}
    } 
    
    private void sendStatusInfo() {
	/* Build Global Status Information (VM + bm_init + Shared slot table */
	/* Send the Global status info to new members */
	SlotsMessagePutStatus msg = new SlotsMessagePutStatus(
            this.slotsTable.clone(), this.initializedNodes.clone(), this.nodeId);
        
        this.println("Send Global status");
	this.broadcast(msg);
    }
    
    private void handleSpreadJoin(SpreadMessageJoin msg) {
        this.println("Handling Spread Join");
        /* update registered nodes table */
        this.registeredNodes = msg.getRegisteredNodes();        
        
        if( msg.getSenderId() == this.nodeId){ /* The own JOIN message	*/
            this.println("Received my own Join message");

            if (this.getActiveNodes() == 1) { 		/* It is a LONELY member*/
                this.println("I'm the first active node");
                
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
            if( (!this.initializedNodes[this.nodeId]) && (this.state != STS_WAIT_INIT)) {
                println("I'm not Inizialized nor waiting init. My State:" +this.getStateAsString());
                return;
            }

            if (primaryMember == nodeId && this.state == STS_RUNNING ) { 	
                sendStatusInfo();
            }
	}
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
                msg.getSenderId(), donatedSlotsList.clone(), this.nodeId);        
        broadcast(donMsg);        
    }
    
    private void handleSlotsDonation(SlotsMessageDonate msg) {
        this.println("Handling Slots Donate from Node#"+msg.getSenderId());
        
        if(!(this.isInitialized(this.nodeId)) && this.state != STS_WAIT_INIT) {
            println("I am not initialized nor waiting initialization. Return.");
            return;
        }

	this.println("Donation of "+msg.getDonatedIdList().length+" slots from "
                +msg.getSenderId()
                +" to "+msg.getRequester());

	/* Is the destination an initialized member ? */
        if(!this.isInitialized(msg.getRequester())) {
            println("WARNING Destination member node#"+msg.getRequester()
                    +" is not initialized");
            return;
	}

	/* Is the donor an initialized member ? */
        if(!this.isInitialized(msg.getSenderId())) {
            println("WARNING Source member node#"+msg.getSenderId()+
                    " is not initialized");
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
            for( int j = 0; j < donatedList.length; j++) {
		int slotId = donatedList[j];
		this.slotsTable[slotId].setStatus(Slot.STATUS_FREE);
            }
            this.state = STS_RUNNING;
	}	

        this.donorsNodes[msg.getSenderId()] = false;
	this.println("free_slots="+this.getFreeSlots()+" free_slots_low="
                +FREE_SLOTS_LOW);
	this.println("owned_slots="+this.getOwnedSlots()+" max_owned_slots="
                +this.maxOwnedSlots+" bm_donors="+ Arrays.toString(donorsNodes));        
    }
    
    private void handleSlotsPutStatus(SlotsMessagePutStatus msg) {
        println("Handling Slots Put Status");
        
        if( this.state != STS_WAIT_STATUS) {
            println("I am not waiting status info. Ignore...");
            return;
        }

        //println("SYS_PUT_STATUS: primarymember="+primaryMember+" table has %d slots");// ret/sizeof(slot_t));
        this.slotsTable = msg.getSlotsTable().clone();
        println("Updating Initialized nodes table...");
        this.initializedNodes = msg.getInitializedNodes().clone();
        this.state = STS_WAIT_INIT;
        
	/* Report to other nodes as INITILIZED */
	println("Multicasting SYS_INITIALIZED");
	broadcast(new SlotsMessageInitialized(this.nodeId));        
 
    }
    
    private boolean isInitialized(int nodeId) {
        return this.initializedNodes[nodeId];
    }
    
    public boolean isInitialized() {
        return this.isInitialized(nodeId);
    }    
    
    private void handleSlotsNewStatus(SlotsMessageNewStatus msg) {
        this.println("Handling Slots New Status");
    }    
    
    private void handleSlotsMergeStatus(SlotsMessageMergeStatus msg) {
        this.println("Handling Slots Merge Status");
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
    
    private int getPrimaryMember() {
        for(int i = 1; i < SlotsDonation.NODES; i++ ) {
            if (this.initializedNodes[i]) {
                println("bm_init="+Arrays.toString(this.initializedNodes)
                        +" primary_mbr="+i);
                return i;
            }
        }
        return NO_PRIMARY_MBR;
    }    
    
    public int getFreeSlots() {
        int counter = 0;
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            if(slotsTable[i].isFree() && slotsTable[i].getOwner() == this.nodeId) {
                counter++;
            }
        }         
        return counter;
    }
    
    public int getUsedSlots() {
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
        println("Sending slot request");	

        /* set donors*/
        this.donorsNodes = this.initializedNodes.clone();
        this.donorsNodes[this.nodeId] = false;

        if(this.countActive(this.donorsNodes) == 0) {
            return;
        }
        
        SlotsMessageRequest msg = new SlotsMessageRequest(nr_slots,
                this.getFreeSlots(), this.getOwnedSlots(), this.nodeId);

        this.broadcast(msg);
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
        return "Node #" + nodeId + "\nStatus: "+ this.getStateAsString() 
                +"\nI own: "+ getSlotsNumber() + "("+this.getFreeSlots()+
                " free) slots\nRegistered Nodes: "
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
            case STS_DISCONNECTED:
                return "Disconnected";            
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