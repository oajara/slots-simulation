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
    
    public static final int MIN_OWNED_SLOTS = 8;
    public static final int FREE_SLOTS_LOW = 8;
    public static final int SLOTS_BY_MSG = 1024;
    
    public static final int MAX_NEW_PROCS = 1;
    
    private static final int MEDIAN_CHANGE_INTERVAL = 20000;
    
    private static final int LT_UNIT = 10;
    private static final int LT_MIN = 1;
    private static final int LT_MAX = 10;    
    
    private static final int FI_MAX = 100;
    private static final int FI_MIN = 1;
    private static final int FI_RANGE = (FI_MAX-FI_MIN+1)/2;
    
    private static final int FI_MIN_AVG = LT_UNIT;
    private static final int FI_MAX_AVG = FI_MAX - LT_UNIT;
    
   
//    public static final int MIN_OWNED_SLOTS = 4;
//    public static final int FREE_SLOTS_LOW = 2;
//    public static final int SLOTS_BY_MSG = 3;    
    
    private final Random random;
    private int arrivalMedian;
    private int nextMedianChange;
    private Slot[] slotsTable = new Slot[SlotsDonation.TOTAL_SLOTS];
//    private Process[] processTable = new Process[SlotsDonation.TOTAL_SLOTS];
    private boolean[] activeNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private boolean[] initializedNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private boolean[] donorsNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private boolean[] bmPendingNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private boolean[] bmWaitSts = new boolean[SlotsDonation.MAX_NODES+1];
    private final int nodeId;
    private int state = STS_DISCONNECTED;
    private int primaryMember;
    private int maxOwnedSlots = SLOTS_BY_MSG;
    private boolean sysBarrier = false;
    private boolean pendingConnect = false;
    
    private boolean gotAtLeastOne = false;
    
    private int counterForksSucceded = 0;
    private int counterForksFailed = 0;
    private int counterExits = 0;
    private int counterConnects = 0;
    private int counterDisconnects = 0;
    private int counterRequestedSlots = 0;
    private int counterGotSlots = 0;
    private int counterDonatedSlots = 0;
    private int counterGotZeroSlots = 0;
    private int[] counterGotFirstSlotAt = new int[SlotsDonation.MAX_NODES-1];
    private int counterAtMessage = 0;
    
    private int timeLeftToFork;
    
    public ProgNormalNode(int id) { 
        this.random = new Random();
        this.nodeId = id;
        for(int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            slotsTable[i] = new Slot(0, Slot.STATUS_FREE, 0);
        } 
 
        this.cleanNodesLists();
    }
    
    @Override
    public void main()  {
        int number;

        //number = this.random.nextInt(SlotsDonation.MAX_NODES); 
        number = this.nodeId * 10; 
        println("Sleeping: "+number);
        sleep(number);
        this.nextMedianChange = MEDIAN_CHANGE_INTERVAL;
        this.arrivalMedian = this.getNextArrivalMedian();
        this.timeLeftToFork = this.getNextForkTime();
	this.doConnect();
        // Start with algorithm
        this.slotsLoop();
    }
    
    public void getInfoLine() {
        //(NodeId, Forks OK, Forks Failed, Exits, TotalRequested, TotalReceived, GotZero)
        System.out.println(""+this.nodeId+','+this.counterForksSucceded+','+
                this.counterForksFailed+','+this.counterExits+','+this.counterRequestedSlots+
                ','+this.counterGotSlots+','+this.counterGotZeroSlots);
    }
    
   
    private void handleSpreadDisconnect(SpreadMessageLeave msg) {
        this.println("Handling Spread Leave");
        
	/* Clear those INITIALIZED members until SYS_PUT_STATUS arrives */
	/* because the primary member will send a new SYS_PUT_STATUS 	*/
	if(this.state == STS_WAIT_STATUS) {
            for(int i = 1; i <= SlotsDonation.MAX_NODES; i++) {
                this.initializedNodes[i] = false;
            }  
            return;
	}        
        
        int disc_mbr = msg.getSenderId();
        
        this.activeNodes = this.cloneBitmapTable(msg.getRegisteredNodes());
        
        if( disc_mbr == this.nodeId){ /* The own LEAVE message	*/
            this.println("Received my own Leave message");
            this.initGlobalVars();
            this.state = STS_DISCONNECTED;
            return;
        }
        
	/* if local_nodeid is not initialized and is the only surviving node , restart all*/
	if(!this.isInitialized() && this.getActiveNodes() == 1) {
		this.initGlobalVars();
		this.doDisconnect();
		this.doConnect();
		return;
	}     
        
	/* I am not initialized yet */
	if( !this.isInitialized() && (this.state != STS_WAIT_INIT))  {
		return;        
        }
        
        /* mark node as not initialized */
        this.initializedNodes[disc_mbr] = false;
        
	/* verify if the local member is waiting a donation from the disconnected node */
        this.donorsNodes[disc_mbr] = false;
        
	
	/* Are there any pending ZERO reply for that dead node ? */
        this.bmPendingNodes[disc_mbr] = false;
        
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
                    this.doDisconnect();
                    this.doConnect();
                    return;
            }		
	}     
        
	/* recalculate global variables */
        this.maxOwnedSlots = (SlotsDonation.TOTAL_SLOTS - 
                (MIN_OWNED_SLOTS * (this.getInitializedNodes() - 1)));
	this.println("max_owned_slots="+this.maxOwnedSlots);        
        
	/* reallocate slots of the disconnecter member to the primary member */
	for(int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            if( this.slotsTable[i].getOwner() == disc_mbr ) {
                /* an uncompleted donation */
                if( this.slotsTable[i].getStatus() == Slot.STATUS_DONATING) { 
                    /* local node was the donor */
                    this.slotsTable[i].setOwner(this.nodeId);
                    this.slotsTable[i].setStatus(Slot.STATUS_FREE);
                } else {
                    if( this.primaryMember == this.nodeId && 
                            this.state == STS_REQ_SLOTS && 
                            this.getOwnedSlots() == 0) {
                        this.state = STS_RUNNING;
                        this.sysBarrier = true;
                    }
                    this.slotsTable[i].setOwner(this.primaryMember);
                }   
            }
	}
        
	/*
	* the disconnected member may be the previous primary_mbr
	* if it leaves the group without finishing new nodes synchronization
	* the new primary_mbr must do that operations.
	*/
	if( this.nodeId == this.primaryMember) {
            //this.sendStatusInfo(); TODO: incomplete (we are not testing with disconnects)
	}
    } 
    
    private boolean amIsender(SpreadMessage msg) {
        return msg.getSenderId() == this.nodeId;
    }

    /*===========================================================================*
     *				sp_join											 *
     * A NEW member has joint the VM group but it is not initialized
     *===========================================================================*/    
    private void handleSpreadJoin(SpreadMessageJoin msg) {
        this.println("Handling Spread Join");
        
        /* update registered nodes table */
        this.activeNodes = this.cloneBitmapTable(msg.getActiveNodes()); 
        
        if (this.getActiveNodes() < 0) {
            return;
        }
                
        if( this.amIsender(msg)){ /* The own JOIN message	*/
            this.println("Received my own Join message");

            if (this.getActiveNodes() == 1) { 		/* It is a LONELY member*/
                this.println("I'm the first active node");

                /* it is ready to start running */
                this.state = STS_RUNNING;
                
                /* it is the Primary Member 	*/
                this.primaryMember = this.nodeId;
                
                this.initializedNodes[this.nodeId] = true;

                /* allocate all slots to the member */
                for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
                    slotsTable[i].setOwner(this.nodeId);
                    slotsTable[i].setStatus(Slot.STATUS_FREE);
                } 
                
                this.sysBarrier = true;
                
            } else {
                /* Waiting Global status info */
                
                this.state = STS_WAIT_STATUS;	
                this.println("New Status: "+this.getStateAsString());
            }
	} else { /* Other node JOINs the group	*/
            println("Other member joines the group: node#"+msg.getSenderId()+". My State="+this.getStateAsString());

            /* I am not initialized yet */
            if( (!this.isInitialized())) {
                println("I'm not Inizialized nor waiting init. My State:" +this.getStateAsString());
                return;
            }
            
            /* if the new member was previously considered as a member of other    */
            /* partition but really had crashed, allocate its slots to primary_mbr */
            /* TODO!!! */
            
            /* This node has sent an SYS_REQ_SLOTS messages, but before the other nodes 	*/
            /* (and itself) could receive slots donation, a VIEW CHANGE (JOIN) has  occurred 	*/
            /* This means that the SYS_REQ_SLOTS message is not STABLE and it was discarded 	*/
            /* Therefore, if the local node hasn't got any owned slot, it must request again 		*/
	    println("bm_donors="+Arrays.toString(this.donorsNodes)+
                    " bm_pending="+Arrays.toString(this.bmPendingNodes));
            this.cleanBinaryList(this.donorsNodes);
            this.cleanBinaryList(this.bmPendingNodes);
            
            /* Sets the bm_waitsts bitmap to signal which new member need to get STATUS from PRIMARY  */
            
            println("member="+this.nodeId+" state="+STS_RUNNING);
            if (this.state == STS_RUNNING ) { 	
                this.bmWaitSts[msg.getSenderId()] = true;

                if(this.primaryMember == this.nodeId) {
                    this.initializedNodes[msg.getSenderId()] = true;
                    this.sendStatusInfo(msg.getSenderId());
                }
                    
            }            

	}
    }

    /*======================================================================*
     *				sp_put_status				*
     * A new member has joined the group. 					*
     * The Primary has broadcasted Global Status information		*
     *======================================================================*/    
    private void handleSlotsPutStatus(SlotsMessagePutStatus msg) {
        this.println("Handling Slots Put Status from Node#"+msg.getSenderId()+" to Node#"+msg.getDestination());
        
        int initMbr = msg.getDestination();
        
        if(initMbr != this.nodeId) {
            if(this.state == STS_RUNNING) {
                this.println("ANULADO!: "+Arrays.toString(this.initializedNodes));
//                this.bmWaitSts[initMbr] = false;
//                println("init_mbr="+initMbr+" bm_waitsts="+Arrays.toString(this.bmWaitSts));
//                if(this.getOwnedSlots() == 0 &&
//                        this.countActive(this.donorsNodes) == 0) {
//                    this.state = STS_REQ_SLOTS;
//                    this.mbrRqstSlots(MIN_OWNED_SLOTS);
//                }
            } else {
                return;
            }
        
            /* Is the new initialized member already considered as Initialized ? */
            if(!this.isInitialized(initMbr)){ 	/* No, set the bitmap and count */
                this.initializedNodes[initMbr] = true;
            } else {
                /* Here comes initialized members without owned slots after a NETWORK MERGE */
                println("WARNING member "+initMbr+" just was initilized");
                return;
            }        
        
            println("init_mbr="+initMbr+" bm_init="+Arrays.toString(this.initializedNodes));

            /* compute the slot high water threshold	*/
            this.maxOwnedSlots = (SlotsDonation.TOTAL_SLOTS - 
                    (MIN_OWNED_SLOTS * (this.getInitializedNodes() - 1)));        
            println("bm_init="+Arrays.toString(this.initializedNodes)+
                    " max_owned_slots="+this.maxOwnedSlots);
            
            return;
        }
        
        
 	/* bm_init considerer the bitmap sent by primary_mbr ORed by 					*/
	/* the bitmap of those nodes initialized before SYS_PUT_STATUS message arrives 	*/        
        boolean[] received = cloneBitmapTable(msg.getInitializedNodes());
        this.println("Received init bitmap: "+Arrays.toString(received));
        for(int i = 1; i <= SlotsDonation.MAX_NODES; i++) {
            if(received[i] == true) {
                this.initializedNodes[i] = true;
            }
        } 
        this.initializedNodes[this.nodeId] = true;
        
        this.println("Updated Initialized nodes table with: "+Arrays.toString(this.initializedNodes));
        
       
        /* Copy the received slot table to local table	*/
        // ESTO ESTA BIEN??
        this.slotsTable = this.cloneSlotTable(msg.getSlotsTable());
        
	/* The member is initialized but it hasn't got slots to start running */
        this.state = STS_REQ_SLOTS;
	this.println("Requesting slots");
        this.mbrRqstSlots(MIN_OWNED_SLOTS);
    }

    
    /*---------------------------------------------------------------------
    *			sp_req_slots
    *   SYS_REQ_SLOTS: A Systask on other node has requested free slots
    *---------------------------------------------------------------------*/
    private void handleSlotsRequest(SlotsMessageRequest msg) {
        int don_nodes, surplus;
        double donated_slots;
        int requester = msg.getSenderId();
        
        println("Handling slot request from Node#"+requester);

        /* the member is  not initialized yet, then it can't respond to this request */
        if( !(this.isInitialized())) {
            println("Cannot donate, I am not initialized");
            return;
        }
        
        /* Ignore owns requests  */
        if(requester == this.nodeId) {
            println("Won't donate to myself. Ignoring...");
            return;
        }        

        /* Verify if the requester is initialized */
        if( !(isInitialized(requester))) {
           println("ERROR: member "+requester+ " was not initilized. Initialized nodes: "+Arrays.toString(this.initializedNodes));
           return;
//            println("HOTFIX: marking node#"+requester+" as initialized");
//            this.initializedNodes[requester] = true;
        }

        /*
        * ALL other initialized members respond to a request slot message 
        * but only members with enough slots will donate
        */
        if( this.state != STS_RUNNING) {
            donated_slots = 0;
        } else {
            if (this.countActive(this.donorsNodes) > 0) {
                println("concurrent request");
                this.donorsNodes[requester] = false;
                return;
            }
            
            don_nodes = this.getInitializedNodes() - 1;
            assert( don_nodes > 0);

            surplus = (this.getFreeSlots() - FREE_SLOTS_LOW);
            println("free_slots="+ this.getFreeSlots()+" free_slots_low="+FREE_SLOTS_LOW
                    +" needed_slots="+msg.getNeedSlots()+" don_nodes="+don_nodes
                    +" surplus="+surplus);
            if( surplus > msg.getNeedSlots()) {  /* donate the slots requested  */
                donated_slots = Math.ceil(msg.getNeedSlots() * surplus / (float)this.maxOwnedSlots);
            } else if( surplus > Math.ceil((FREE_SLOTS_LOW/(float)don_nodes))) {
                /* donate slots at least up to complete the minimal number of free slots */
                donated_slots = Math.ceil((FREE_SLOTS_LOW/(float)don_nodes));
            } else if( surplus > 0 ) { /* donate at least one */
                donated_slots = 1;
            } else {
                donated_slots = 0;
            }
        }
        
	/* if the local node does not have slots to donate and have no pending donation response to requester*/
	/* it tries to reply later after receiving a donation message to the requester from other node		*/
	/* WARNING: if any node has slots to donate, the requester will hang waiting for donations which 	*/
	/* never will come. To break this issue, the requester's next member always 	replies  			*/

        if (donated_slots == 0 && this.bmPendingNodes[requester] == false) {
            /* next member always reply */
            if(this.getNextMbr(requester) != this.nodeId) {
                this.bmPendingNodes[requester] = true;
                this.println("Delaying reply to "+requester);
                return;
            } 
            this.println("I'm next member. Replying with zero slots to Node#"+ requester);
        }

        /* Maximum number of slots that can be donated by message round - Limited by the message structure */
        if(donated_slots > SLOTS_BY_MSG) {
            donated_slots = SLOTS_BY_MSG;
        }
        this.println("Donated_slots="+donated_slots+" requester="+requester);
        
 
        /* build the list of donated slots */
        int[] donatedSlotsList = new int[(int)donated_slots];
        for(int r = 0; r < donated_slots; r++) {
            donatedSlotsList[r] = 0;
        }
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
        this.counterDonatedSlots = donatedSlotsList.length + this.counterDonatedSlots;
        broadcast(donMsg);        
    }
        
    /*----------------------------------------------------------------------------------------------------
    *				sp_don_slots
    *   SYS_DON_SLOTS: A Donation Message has received
    *  If it is the destination systask, it adds the new free slots to its own
    * or if they are for other node register the new ownership
    *----------------------------------------------------------------------------------------------------*/    
    private void handleSlotsDonation(SlotsMessageDonate msg) {
        this.println("Handling Slots Donate from Node#"+msg.getSenderId());
        
        if(!(this.isInitialized())) {
            println("I am not initialized nor waiting initialization. Return.");
            return;
        }

	this.println("Donation of "+msg.getDonatedIdList().length+" slots from "
                +msg.getSenderId()
                +" to "+msg.getRequester());

	/* Is the destination an initialized member ? */
        if(!this.isInitialized(msg.getRequester())) {
//            println("WARNING Destination member node#"+msg.getRequester()
//                    +" is not initialized");
//            return;
            println("HOTFIX: marking node#"+msg.getRequester()+" as initialized");
            this.initializedNodes[msg.getRequester()] = true;
	}

	/* Is the donor an initialized member ? */
        if(!this.isInitialized(msg.getSenderId())) {
//            println("WARNING Source member node#"+msg.getSenderId()+
//                    " is not initialized");
//            return;
            println("HOTFIX: marking node#"+msg.getSenderId()+" as initialized");
            this.initializedNodes[msg.getSenderId()] = true;            
	}        

        int[] donatedList = msg.getDonatedIdList().clone();
        
        /* this is for me */
	if( this.state == STS_REQ_SLOTS && donatedList.length > 0 
                && msg.getRequester() == this.nodeId) {
            this.state = STS_RUNNING;
            if( this.getOwnedSlots() == 0){ /* a new member without slots */
                    this.println("Wake up fork/exit");
                    this.sysBarrier = true;	/* Wakeup SYSTASK 		*/	
            }
	}	
        
	/* Change the owner of the donated slots */
	for( int j = 0; j < donatedList.length; j++) {
		int slotId = donatedList[j];
		this.slotsTable[slotId].setOwner(msg.getRequester());
                this.slotsTable[slotId].setStatus(Slot.STATUS_FREE);
                if(msg.getRequester() == this.nodeId){
                    this.counterGotSlots++;
                    if(!this.gotAtLeastOne) {
                        this.gotAtLeastOne = true;
                        this.counterGotFirstSlotAt[this.counterAtMessage]++;
                    } 
                    
                }
        }
        
        if(msg.getRequester() == this.nodeId){
            this.counterAtMessage++;
        }

	/* If the slots are for other node, returns */
        if(msg.getRequester() != this.nodeId){
            if(this.bmPendingNodes[msg.getRequester()] == true) {
                this.println("Delayed reply to Node#"+msg.getRequester());
                int[] donatedSlotsList = new int[0];
                SlotsMessageDonate donMsg = new SlotsMessageDonate(
                msg.getRequester(), donatedSlotsList.clone(), this.nodeId);
                broadcast(donMsg);
                this.bmPendingNodes[msg.getRequester()] = false;
            }
            return;
        }
        
//	if( spin_ptr->mA_dst != local_nodeid) {
//		rcode = OK;
//		/* Are there any pending reply with ZERO slots for this destination ?*/
//		if( TEST_BIT(bm_pending, spin_ptr->mA_dst)) {
//			donated_slots = 0;
//			requester = spin_ptr->mA_dst;
//			TASKDEBUG("procs=%d donated_slots=%d requester=%d\n",
//				(vm_ptr->vm_nr_tasks + vm_ptr->vm_nr_procs),donated_slots, requester);
//			spout_ptr->m_source = local_nodeid;
//			spout_ptr->m_type = SYS_DON_SLOTS;
//			spout_ptr->mA_dst = requester;
//			spout_ptr->mA_nr = donated_slots;
//			for ( j = 0 ; j < SLOTS_BY_MSG; j++) spout_ptr->mA_ia[j] = 0;
//			rcode = SP_multicast (sysmbox, SAFE_MESS, (char *) vm_ptr->vm_name,
//				SYS_DON_SLOTS, sizeof(message), (char *) spout_ptr);
//			CLR_BIT(bm_pending, spin_ptr->mA_dst);
//		}
//		return(rcode);
//	}        

        this.donorsNodes[msg.getSenderId()] = false;
	this.println("free_slots="+this.getFreeSlots()+" free_slots_low="
                +FREE_SLOTS_LOW);
	this.println("owned_slots="+this.getOwnedSlots()+" max_owned_slots="
                +this.maxOwnedSlots+" bm_donors="+ Arrays.toString(donorsNodes)); 
        
        if( this.state == STS_REQ_SLOTS && this.countActive(this.donorsNodes) == 0
                && this.getOwnedSlots() == 0) {
            mbrRqstSlots(MIN_OWNED_SLOTS);
        }
        
        if (this.countActive(this.donorsNodes) == 0 && this.gotAtLeastOne == false) {
            this.counterGotZeroSlots++;
        } 
        
    }
    
    
    private void handleSlotsMergeStatus(SlotsMessageMergeStatus msg) {
        this.println("Handling Slots Merge Status (TODO)");
    }        
    
    private void sendStatusInfo(int destId) {
	/* Build Global Status Information (VM + bm_init + Shared slot table */
	/* Send the Global status info to new members */
	SlotsMessagePutStatus msg = new SlotsMessagePutStatus(
            this.cloneSlotTable(this.slotsTable), cloneBitmapTable(this.initializedNodes), this.nodeId, destId);
        
        this.println("Broadcasting Global status from Node#"+this.nodeId+"to Node#"+destId);
	this.broadcast(msg);
    }
        
    /*===========================================================================*
     *				mbr_rqst_slots					   
     * It builds and broadcasts a message requesting slots
     *===========================================================================*/
    private void mbrRqstSlots(int nr_slots) {
        this.println("Sending request of slots: " + nr_slots);	

        /* set donors*/
        this.donorsNodes = cloneBitmapTable(this.initializedNodes);
        this.donorsNodes[this.nodeId] = false;

        if(this.countActive(this.donorsNodes) == 0) {
            return;
        }
        
        SlotsMessageRequest msg = new SlotsMessageRequest(nr_slots,
                this.getFreeSlots(), this.getOwnedSlots(), this.nodeId);
        this.counterRequestedSlots = nr_slots + this.counterRequestedSlots;
        this.gotAtLeastOne = false;
        this.counterAtMessage = 0;
        this.broadcast(msg);
    } 
    
    /****************
     * AUXILIARY
     ****************/
    
   /**
     * Broadcasting a message is sending it to the spread node
     * @param msg 
     */
    public void broadcast(Message msg) {
        out(0).send(msg);
    }
 
    private void slotsLoop() {
        Message msg;
        int mbr;
        while(true) {
//            this.println("Waiting for message...");
                msg = this.in(0).receive(1);
            if (msg != null) {
                if(msg instanceof SpreadMessage) {
                    if(msg instanceof SpreadMessageJoin) {
                        this.handleSpreadJoin((SpreadMessageJoin)msg);
                    } else if (msg instanceof SpreadMessageLeave) {
                        this.handleSpreadDisconnect((SpreadMessageLeave)msg);
                    } else {
                        this.println("THIS SHOULD NOT HAPPEN!!! SpreadMessage non JOIN or LEAVE!");
                    }                    
                } else if (this.isConnected()) {
                    if(msg instanceof SlotsMessageRequest) {
                        this.handleSlotsRequest((SlotsMessageRequest)msg);
                    } else if (msg instanceof SlotsMessageDonate) {
                        this.handleSlotsDonation((SlotsMessageDonate)msg);
                    } else if (msg instanceof SlotsMessagePutStatus) {
                        mbr = ((SlotsMessagePutStatus)msg).getSenderId();
                        if( !this.isInitialized()) {
                            this.primaryMember = mbr;
			} else {
                            if(this.primaryMember != mbr) {
                                this.println("SYS_PUT_STATUS: current primary_mbr="+
                                        this.primaryMember+" differs from new primary_mbr="+
                                        mbr);
                            }
                            if(!this.isInitialized(mbr)) {
                                this.println("SYS_PUT_STATUS: primary_mbr="+mbr+" is not in bm_init="+Arrays.toString(this.initializedNodes));
                            }
			}
                        this.handleSlotsPutStatus((SlotsMessagePutStatus)msg);
                    } else if (msg instanceof SlotsMessageMergeStatus) {
                        this.handleSlotsMergeStatus((SlotsMessageMergeStatus)msg);
                    } else {
                        this.println("UNHANDLED SLOT MESSAGE!!!");
                    }                    
                } else {
                    this.println("Ignoring Slots Message. Node is not connected!");
                }
            }
            
            //check fork or exit
            this.processForkExit();
//            test((GlobalAssertion)this.slotsAssertion);
        }    
    }
 
    
    @Override
    public String getText() {
        return "Node #" + nodeId + "\nStatus: "+ this.getStateAsString() 
                +"\nI own: "+ this.getOwnedSlots() + " ("+this.getFreeSlots()+
                " free) slots\nmaxOwnedSlots: "+this.maxOwnedSlots+"\n"
                + "FSL: "+FREE_SLOTS_LOW+ "\nRegistered Nodes: "
                + Arrays.toString(this.activeNodes)+"\nInitialized Nodes: "
                + Arrays.toString(this.initializedNodes)+"\nDonor Nodes: "
                + Arrays.toString(this.donorsNodes)+"\nForks Succeded: "+this.counterForksSucceded
                +"\nForks Failed: "+this.counterForksFailed
                +"\nExits: "+this.counterExits+"\nConnects: "+this.counterConnects
                +"\nDisconnects: "+this.counterDisconnects
                +"\nRequested Slots: "+this.counterRequestedSlots
                +"\nDonated To Me Slots: "+this.counterGotSlots
                +"\nDonated By Me Slots: "+this.counterDonatedSlots
                +"\nArrival Median: "+this.arrivalMedian
                +"\nNext Fork in: "+this.timeLeftToFork;
    }
    
    public void println(String str) {
        System.out.println("Node[" + nodeId + "]("+getTime()+"): "+str);
    }    
    
    public void decProcessesLifetimes() {
        int counter = 0;
        for(int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            if(slotsTable[i].isUsed() && slotsTable[i].getOwner() == this.nodeId) {
                if(slotsTable[i].processTimeLeft == 0) {
                    println("Killing process["+i+"]");
                    this.doExit(i);    
                    counter++;
                } else {
                    slotsTable[i].processTimeLeft--;
                }
            }
        }  
//        if (counter > 0)
 //           this.println("Number of destroyed processes: " + counter);
//        this.println("Process Table: "+Arrays.toString(this.processTable));
    }
    
    /** find first owned free slot and use it **/
    private void markSlotUsed() {
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            if(slotsTable[i].isFree() && slotsTable[i].getOwner() == this.nodeId) {
                slotsTable[i].setStatus(Slot.STATUS_USED);
                slotsTable[i].setProcessLifetime(this.getRandomProcessLifeTime());
                println("Created a process["+i+"] of lifetime "+slotsTable[i].processTimeLeft);
                return;
            }
        }           
    }

    private int getPrimaryMember() {
        for(int i = 1; i < SlotsDonation.NODES; i++ ) {
            if (this.isInitialized(i)) {
                println("bm_init="+Arrays.toString(this.initializedNodes)
                        +" primary_mbr="+i);
                return i;
            }
        }
        return NO_PRIMARY_MBR;
    }    
    
    int getNextMbr(int node) {
        int i, next_mbr;

//        TASKDEBUG("node=%d\n", node);

        next_mbr = NO_PRIMARY_MBR;
//        assert( node < drvs_ptr->d_nr_nodes);
        if( this.getInitializedNodes() == 1) return(next_mbr);

        for(i = 1, next_mbr = node; i < SlotsDonation.NODES; i++ ) {
            next_mbr = (next_mbr + 1) % SlotsDonation.NODES;
            if (this.isInitialized(next_mbr)) {
                println("next="+next_mbr);
                return(next_mbr);
            }
        }
        return NO_PRIMARY_MBR;
    }    
   
 
    private boolean isInitialized(int nodeId) {
        return this.initializedNodes[nodeId];
    }
    
    public boolean isInitialized() {
        return this.isInitialized(this.nodeId);
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
            case STS_REQ_SLOTS:
                return "Requested Slots";   
            case STS_NEW:
                return "New ??";   
            case STS_MERGE_STATUS:
                return "Merge ??";                  
            default:
                return "Unknown Status: "+this.state;
        }
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
    
    public int getOwnedSlots() {
        int counter = 0;
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            if(slotsTable[i].getOwner() == this.nodeId) {
                counter++;
            }
        }         
        return counter;
    }       

    private void processForkExit() {
        if(this.nextMedianChange == 0) {
            this.arrivalMedian = this.getNextArrivalMedian();
            this.nextMedianChange = MEDIAN_CHANGE_INTERVAL;
            println("Changed arrival median to "+ this.nextMedianChange);
        } else {
            this.nextMedianChange--;
        }
        
        this.timeLeftToFork--;
        
        if (this.timeLeftToFork == 0) { // time for a new fork
            this.timeLeftToFork = this.getNextForkTime();
            if(this.isConnected() && this.isInitialized() && this.sysBarrier) {
                this.tryFork();
            }
        }
            
        if(this.isConnected() && this.isInitialized()) {
            this.decProcessesLifetimes();
        }                        
    }
    
    private boolean isConnected() {
        return (this.activeNodes[this.nodeId]);
    }
    
    private void doConnect() {
        println("Connecting...");
        this.pendingConnect = true;
        this.counterConnects++;
        out(0).send(new SpreadMessageJoin(this.nodeId));
    }
    
    private void doDisconnect() {
        println("Disconnecting...");
        this.initGlobalVars();
        this.state = STS_DISCONNECTED;
        this.counterDisconnects++;
        out(0).send(new SpreadMessageLeave(this.nodeId));
    }    
    
    private int getActiveNodes() {
        int counter = 0;
        for(int i = 0; i <= SlotsDonation.MAX_NODES; i++) {
            if(this.activeNodes[i] == true) {
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
        this.cleanNodesLists();    
        this.cleanSlotsLists();
        
        this.cleanCounterGotFirstAt();
        this.sysBarrier = false;
        this.pendingConnect = false;
        this.maxOwnedSlots = 0;
        
        this.cleanBinaryList(this.bmWaitSts);
    }
    
    private void cleanBinaryList(boolean[] list) {
        for(int i = 1; i < list.length; i++) {
            list[i] = false;
        }
    }
    
    private void cleanNodesLists() {
        for(int i = 1; i <= SlotsDonation.MAX_NODES; i++) {
            this.initializedNodes[i] = false;
            this.activeNodes[i] = false;
            this.donorsNodes[i] = false;
            this.bmPendingNodes[i] = false;
        }          
    } 
    
    private void cleanCounterGotFirstAt() {
        for(int i = 0; i < SlotsDonation.MAX_NODES-1; i++) {
            this.counterGotFirstSlotAt[i] = 0;
        }          
    }     
    
    private void cleanSlotsLists() {
        for(int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            this.slotsTable[i] = new Slot();
        }          
    }    
    
    private Slot[] cloneSlotTable(Slot[] table) {
        Slot[] cloneTable = new Slot[SlotsDonation.TOTAL_SLOTS];
        for(int i = 0; i<SlotsDonation.TOTAL_SLOTS; i++) {
            cloneTable[i] = new Slot(table[i]);
        }
        return cloneTable;
    }
    
    private boolean[] cloneBitmapTable(boolean[] table) {
        boolean[] cloneTable;
        cloneTable = Arrays.copyOf(table, SlotsDonation.MAX_NODES+1);
        return cloneTable;       
    }
    
    public boolean tryFork() {
        int leftover;
        //this.println("Executing a fork");
        
	if( this.getFreeSlots() < FREE_SLOTS_LOW ) {
            this.println("I'm below FREE_SLOTS_LOW");
            if( this.getOwnedSlots() < this.maxOwnedSlots ) { /* do I achieve the maximum threshold  ? */
                if(this.countActive(this.donorsNodes) == 0) { 	/*  if there pending donation requests? */
                    if( this.getOwnedSlots() < Math.ceil(this.maxOwnedSlots/this.getInitializedNodes()))
//			leftover =  CEILING(max_owned_slots,init_nodes) - owned_slots;
                        leftover = ((int)Math.ceil(this.maxOwnedSlots/getInitializedNodes())) - getOwnedSlots();
                    else
                        leftover = FREE_SLOTS_LOW - getFreeSlots();
                    this.println("leftover="+leftover+" free_slots="+
                            this.getFreeSlots()+" free_slots_low="+FREE_SLOTS_LOW+
                            " bm_donors="+Arrays.toString(this.donorsNodes));
                    this.println("owned_slots="+this.getOwnedSlots()+
                            " max_owned_slots="+this.maxOwnedSlots);
                    this.println("Requesting slot donation");
                    this.mbrRqstSlots(leftover);
                } else {
                    this.println("but there's at least one pending donor: "+Arrays.toString(this.donorsNodes));
                }
            } else {
                this.println("I'm already over maxOwnedSlots");
            }
        } 
	
	if(this.getFreeSlots() == 0) {
            this.println("No frees slot :(");
            this.counterForksFailed++;
            return(false);
	} else {
            //println("Free slot found, forking");
            this.markSlotUsed();
            this.counterForksSucceded++;
            return (true);
        } 
        
        
    }
    
    public void doExit(int s) {
        //println("Executing exit of a process");
        if(slotsTable[s].isUsed() && slotsTable[s].getOwner() == this.nodeId) {
            slotsTable[s].setStatus(Slot.STATUS_FREE);
        } else {
            this.println("ERROR: this slot is not owned by node or is not used: "
                    + s);
        }
        this.counterExits++;
    }
   
    public int[] getCounterGotFirstAt() {
        return this.counterGotFirstSlotAt;
    }

    private int getNextForkTime() {
        double val, next_float;
        
//        do {
//            val =  this.random.nextGaussian() * 50;
//            /*
//            (-50;50) el 70% de las veces (std dev 1)
//            (-100;100) el 95% de las veces
//            */
//            next_float = val + this.arrivalMedian; // median correction
//        } while (next_float < 0 || next_float > 100); // limits
        
        val =  this.random.nextGaussian() * FI_RANGE;
        /*
        (-50;50) el 70% de las veces (std dev 1)
        (-100;100) el 95% de las veces
        */
        next_float = val + this.arrivalMedian; // median correction
        
        // limits
        next_float = next_float < FI_MIN ? FI_MIN : next_float;
        next_float = next_float > FI_MAX ? FI_MAX : next_float;

        return ((int) Math.round(next_float));      
    }
    
    private int getRandomProcessLifeTime() {
        //int lt = MIN_PLIFETIME + this.random.nextInt(MAX_PLIFETIME - MIN_PLIFETIME + 1);
        
        double val = this.random.nextFloat();
                
        for (int i = 1 + LT_MIN ; i<LT_MAX; i++) {
            if(val < 1-(1/i)) {
                return (i - 1) * LT_UNIT;
            }
        }
        
        return (LT_MAX * LT_UNIT);
    }       
    
    /**
     * Returns a uniform random median in the interval [10;90]
     * @return 
     */
    private int getNextArrivalMedian() {
        int median = FI_MIN_AVG + this.random.nextInt(FI_MAX_AVG - FI_MIN_AVG + 1);
        return (median);
    }
    
}