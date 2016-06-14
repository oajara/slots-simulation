// NORMAL SLOT NODE - PAP 20160529

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
    public static final int FREE_SLOTS_LOW = 4;
    public static final int SLOTS_BY_MSG = 1024;

    public static final int MAX_NEW_PROCS = 1;

    public static final int LT_UNIT = 45;
    public static final int LT_MIN = 1;
    public static final int LT_MAX = 100;

    public static final int FI_MAX = 10;
    public static final int FI_MIN = 1;
    public static final int FI_RANGE = 4;

    public static final int FI_MIN_AVG = 5;
    public static final int FI_MAX_AVG = 10;

    public static final int MEDIAN_CHANGE_INTERVAL = 5000;


    private final Random random;
    private int arrivalMedian;
    private int nextMedianChange;
    //    implemented slotTable on the parent class
    private Slot[] slotsTable = new Slot[SlotsDonation.TOTAL_SLOTS];
    //    private Process[] processTable = new Process[SlotsDonation.TOTAL_SLOTS];
    private boolean[] activeNodes = new boolean[SlotsDonation.MAX_NODES + 1];
    private boolean[] initializedNodes = new boolean[SlotsDonation.MAX_NODES + 1];
    private boolean[] donorsNodes = new boolean[SlotsDonation.MAX_NODES + 1];
    private boolean[] bmPendingNodes = new boolean[SlotsDonation.MAX_NODES + 1];
    private boolean[] bmWaitSts = new boolean[SlotsDonation.MAX_NODES + 1];
    private final int nodeId;
    private int state = STS_DISCONNECTED;
    private int primaryMember;
    private int maxOwnedSlots = SlotsDonation.TOTAL_SLOTS;
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
    private int[] counterGotFirstSlotAt = new int[SlotsDonation.MAX_NODES - 1];
    private int counterAtMessage = 0;

    private int timeLeftToFork;

    public ProgNormalNode(int id) {
        this.random = new Random();
        this.nodeId = id;
        for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            slotsTable[i] = new Slot(0, Slot.STATUS_FREE, 0);
        }

        this.cleanNodesLists();
    }

    @Override
    public void main() {
        int number;

        number = this.nodeId * 10;
        println("Sleeping: " + number);
        sleep(number);
        this.nextMedianChange = MEDIAN_CHANGE_INTERVAL;
        this.arrivalMedian = this.getNextArrivalMedian();
        this.timeLeftToFork = getTime() + this.getNextDeltaFork();
        this.doConnect();
        // Start with algorithm
        this.slotsLoop();
    }

    public void getInfoLine() {
        //(NodeId, Forks OK, Forks Failed, Exits, TotalRequested, TotalReceived, GotZero)
        System.out.println("" + this.nodeId + ',' + this.counterForksSucceded + ',' +
                this.counterForksFailed + ',' + this.counterExits + ',' + this.counterRequestedSlots +
                ',' + this.counterGotSlots + ',' + this.counterGotZeroSlots);
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

        if (this.amIsender(msg)) { /* The own JOIN message	*/
            this.println("Received my own Join message");

            if (this.getActiveNodes() == 1) { 		/* It is a LONELY member*/
                this.println("I'm the first active node");

                /* it is ready to start running */
                this.state = STS_RUNNING;

                /* it is the Primary Member 	*/
                this.primaryMember = this.nodeId;

                this.initializedNodes[this.nodeId] = true;

                this.sysBarrier = true;

            } else {
                /* Waiting Global status info */

                this.state = STS_WAIT_STATUS;
                this.println("New Status: " + this.getStateAsString());
            }
        } else { /* Other node JOINs the group	*/
            println("Other member joined the group: node#" + msg.getSenderId() + ". My State=" + this.getStateAsString());

            /* I am not initialized yet */
            if ((!this.isInitialized())) {
                println("I'm not Inizialized nor waiting init. My State:" + this.getStateAsString());
                return;
            }

            println(" bm_pending=" + Arrays.toString(this.bmPendingNodes));

            this.cleanBinaryList(this.bmPendingNodes);

            /* Sets the bm_waitsts bitmap to signal which new member need to get STATUS from PRIMARY  */

            println("member=" + this.nodeId + " state=" + STS_RUNNING);
            if (this.state == STS_RUNNING) {
                if (this.primaryMember == this.nodeId) {
                    if (this.getWaitStsNodes() == 0) {
                        this.sendStatusInfo(msg.getSenderId());
                    }
                }
                this.bmWaitSts[msg.getSenderId()] = true;
            }
        }
    }

    private void handleSlotsRequest(SlotsMessageRequest msg) {

        int requester = msg.getSenderId();

        println("Handling slot request from Node#"+requester);


//        si es el nodo primario
        if (this.getPrimaryMember() == this.nodeId){
            /* Verify if the requester is initialized */
            if( !(isInitialized(requester))) {
                println("ERROR: member "+requester+ " was not initilized. Initialized nodes: "+Arrays.toString(this.initializedNodes));
                return;

            }
            //      la variable sera el primer slot libre o -1 en caso de que no halla
            int free = this.getFirstFree();
            if(free > -1){
                this.updateTable(free, requester);
                SlotsMessageTable newTable = new SlotsMessageTable(0, slotsTable);
                this.broadcast(newTable);
//                sysBarrier = false;
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
            this.bmWaitSts[initMbr] = false;
            println("init_mbr="+initMbr+" bm_waitsts="+Arrays.toString(this.bmWaitSts));
            if(this.state == STS_RUNNING) {
                if(this.primaryMember == this.nodeId) {
                    if (this.getWaitStsNodes() != 0) {
                        int next_wait = getNextWait(0);
                        if (next_wait != NO_PRIMARY_MBR) {
                            this.sendStatusInfo(next_wait);
                        }
                    }
                }
            } else if (this.getOwnedSlots() == 0 ) {
                if (this.getWaitStsNodes() == 0) {
                    this.state = STS_REQ_SLOTS;
                    if(this.getInitializedNodes() > 1 )
                        this.mbrRqstSlots();
                }
            } else {
                return;
            }

            /* Is the new initialized member already considered as Initialized ? */
            if(!this.isInitialized(initMbr)) { 	/* No, set the bitmap and count */
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

            /* IMPLICIT SYS_REQ_SLOTS when JOIN->PUT_STATUS  */
            // PAP: CONSTRUIR UN MENSAJE con parametro initMbr como source
            // y MIN_OWNED_SLOTS como getNeedSlots
            // para poder invocar a la funcion.
            SlotsMessageRequest newMsg = new SlotsMessageRequest(initMbr);
            this.handleSlotsRequest(newMsg);

            return;
        }

        /*
        *   INIT_MBR is  LOCAL NODE
        *The init_mbr has receipt a SYS_PUT_STATUS message, therefore does not need anymore
        */

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
	/* IMPLICIT SYS_REQ_SLOTS when JOIN->PUT_STATUS  */
    }

    private void handleUpdateTable (SlotsMessageTable msg){
        System.arraycopy(msg.getTable(),0,slotsTable,0,SlotsDonation.TOTAL_SLOTS);
        println("slots owned: "+ this.getOwnedSlots());
    }


    private void handleSlotsMergeStatus(SlotsMessageMergeStatus msg) {
        this.println("Handling Slots Merge Status (TODO)");
    }

    private void sendStatusInfo(int destId) {
	/* Build Global Status Information (VM + bm_init + Shared slot table */
	/* Send the Global status info to new members */
        SlotsMessagePutStatus msg = new SlotsMessagePutStatus(
                this.cloneSlotTable(this.slotsTable), cloneBitmapTable(this.initializedNodes), this.nodeId, destId);

        this.println("Broadcasting Global status from Node#" + this.nodeId + "to Node#" + destId);
        this.broadcast(msg);
    }

    /*===========================================================================*
     *				mbr_rqst_slots
     * It builds and broadcasts a message requesting slots
     *===========================================================================*/
    private void mbrRqstSlots() {
        this.println("Sending request of a slot. ");
        int nr_slots = 1;

        /* TODO: cambiar donar slots por verificar si hay alguno y darlo */

        SlotsMessageRequest msg = new SlotsMessageRequest(this.nodeId);
        this.counterRequestedSlots = nr_slots + this.counterRequestedSlots;
        this.gotAtLeastOne = false;
        this.counterAtMessage = 0;
        this.state = STS_REQ_SLOTS;
        this.broadcast(msg);
    }

    /****************
     * AUXILIARY
     ****************/

    /**
     * Broadcasting a message is sending it to the spread node
     *
     * @param msg
     */
    public void broadcast(Message msg) {
        out(0).send(msg);
    }

    private void slotsLoop() {
        Message msg;
        int mbr;
        while (true) {
//            this.println("Waiting for message...");
            msg = this.in(0).receive(1);
            if (msg != null) {
                if (msg instanceof SpreadMessage) {
                    if (msg instanceof SpreadMessageJoin) {
                        this.handleSpreadJoin((SpreadMessageJoin) msg);
                    } else if (msg instanceof SpreadMessageLeave) {
                        this.handleSpreadDisconnect((SpreadMessageLeave) msg);
                    } else {
                        this.println("THIS SHOULD NOT HAPPEN!!! SpreadMessage non JOIN or LEAVE!");
                    }
                } else if (this.isConnected()) {
                    if (msg instanceof SlotsMessageRequest) {
                        this.handleSlotsRequest((SlotsMessageRequest) msg);
                    }
                    else if (msg instanceof SlotsMessagePutStatus) {
                        mbr = ((SlotsMessagePutStatus) msg).getSenderId();
                        if (!this.isInitialized()) {
                            this.primaryMember = mbr;
                        } else {
                            if (this.primaryMember != mbr) {
                                this.println("SYS_PUT_STATUS: current primary_mbr:" +
                                        this.primaryMember + " differs from new primary_mbr:" +
                                        mbr);
                            }
                            if (!this.isInitialized(mbr)) {
                                this.println("SYS_PUT_STATUS: primary_mbr:" + mbr + " is not in bm_init:" + Arrays.toString(this.initializedNodes));
                            }
                        }
                        this.handleSlotsPutStatus((SlotsMessagePutStatus) msg);
                    } else if (msg instanceof SlotsMessageMergeStatus) {
                        this.handleSlotsMergeStatus((SlotsMessageMergeStatus) msg);
                    } else if (msg instanceof SlotsMessageTable){
                        this.handleUpdateTable((SlotsMessageTable) msg);
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
        return "Node #" + nodeId + "\nStatus: " + this.getStateAsString()
                + "\nI own: " + this.getOwnedSlots() + " (" + this.getFreeSlots() +
                " free) slots\nmaxOwnedSlots: " + this.maxOwnedSlots + "\n"
                + "FSL: " + FREE_SLOTS_LOW + "\nRegistered Nodes: "
//                + Arrays.toString(this.activeNodes) + "\nInitialized Nodes: "
//                + Arrays.toString(this.donorsNodes) + "\nForks Succeded: " + this.counterForksSucceded
                + "\nForks Failed: " + this.counterForksFailed
                + "\nExits: " + this.counterExits + "\nConnects: " + this.counterConnects
                + "\nDisconnects: " + this.counterDisconnects
                + "\nRequested Slots: " + this.counterRequestedSlots
                + "\nArrival Median: " + this.arrivalMedian
                + "\nNext Fork: " + this.timeLeftToFork
                + "\nCurrent Time: " + getTime();
    }

    public void println(String str) {
        System.out.println("Node[" + nodeId + "](" + getTime() + "): " + str);
    }

    public void decProcessesLifetimes() {
        int counter = 0;
        for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            if (slotsTable[i].isUsed() && slotsTable[i].getOwner() == this.nodeId) {
                if (slotsTable[i].processTimeLeft == 0) {
                    println("Killing process: " + i);
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

    /**
     * find first owned free slot and use it
     **/
    private void markSlotUsed() {
        for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            if (slotsTable[i].isFree() && slotsTable[i].getOwner() == this.nodeId) {
                slotsTable[i].setStatus(Slot.STATUS_USED);
                slotsTable[i].setProcessLifetime(this.getRandomProcessLifeTime());
                println("Created a process[" + i + "] of lifetime " + slotsTable[i].processTimeLeft);
                println("slots owned: "+ this.getOwnedSlots());
                return;
            }
        }
    }

    private int getPrimaryMember() {
        for (int i = 1; i < SlotsDonation.NODES; i++) {
            if (this.isInitialized(i)) {
                println("bm_init:" + Arrays.toString(this.initializedNodes)
                        + " primary_mbr:" + i);
                return i;
            }
        }
        return NO_PRIMARY_MBR;
    }

    int getNextWait(int node) {
        int i, wait_mbr;

//        TASKDEBUG("node=%d\n", node);

        wait_mbr = NO_PRIMARY_MBR;
//        assert( node < drvs_ptr->d_nr_nodes);
        if (this.getInitializedNodes() == 1) return (wait_mbr);

        for (i = 1, wait_mbr = node; i < SlotsDonation.NODES; i++) {
            wait_mbr = (wait_mbr + 1) % SlotsDonation.NODES;
            if (this.bmWaitSts[wait_mbr]) {
                println("next:" + wait_mbr);
                return (wait_mbr);
            }
        }
        return NO_PRIMARY_MBR;
    }

    int getNextInit(int node) {
        int i, next_mbr;

//        TASKDEBUG("node=%d\n", node);

        next_mbr = NO_PRIMARY_MBR;
//        assert( node < drvs_ptr->d_nr_nodes);
        if (this.getInitializedNodes() == 1) return (next_mbr);

        for (i = 1, next_mbr = node; i < SlotsDonation.NODES; i++) {
            next_mbr = (next_mbr + 1) % SlotsDonation.NODES;
            if (this.isInitialized(next_mbr)) {
                println("next:" + next_mbr);
                return (next_mbr);
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
        switch (this.state) {
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
                return "Unknown Status: " + this.state;
        }
    }

    private int countActive(boolean[] list) {
        int counter = 0;
        for (int i = 0; i < list.length; i++) {
            if (list[i]) {
                counter++;
            }
        }
        return counter;

    }

    public int getFreeSlots() {
        int counter = 0;
        for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            if (slotsTable[i].isFree() && slotsTable[i].getOwner() == this.nodeId) {
                counter++;
            }
        }
        return counter;
    }

    public int getUsedSlots() {
        int counter = 0;
        for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            if (slotsTable[i].isUsed() && slotsTable[i].getOwner() == this.nodeId) {
                counter++;
            }
        }
        return counter;
    }

    public int getOwnedSlots() {
        int counter = 0;
        for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            if (slotsTable[i].getOwner() == this.nodeId) {
                counter++;
            }
        }
        return counter;
    }

    private void processForkExit() {
        if (this.nextMedianChange == 0) {
            this.arrivalMedian = this.getNextArrivalMedian();
            this.nextMedianChange = MEDIAN_CHANGE_INTERVAL;
            println("Changed fork arrival median to: " + this.arrivalMedian);
        } else {
            this.nextMedianChange--;
        }

        if (this.timeLeftToFork <= getTime()) { // time for a new fork
            this.timeLeftToFork = getTime() + this.getNextDeltaFork();
            //println("Next fork in: "+this.timeLeftToFork);
            if (this.isConnected() && this.isInitialized() && this.sysBarrier) {
                this.tryFork();
            }
        }

        if (this.isConnected() && this.isInitialized()) {
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
        for (int i = 0; i <= SlotsDonation.MAX_NODES; i++) {
            if (this.activeNodes[i] == true) {
                counter++;
            }
        }
        return counter;
    }

    private int getInitializedNodes() {
        int counter = 0;
        for (int i = 1; i <= SlotsDonation.MAX_NODES; i++) {
            if (this.initializedNodes[i]) {
                counter++;
            }
        }
        return counter;
    }

    private int getWaitStsNodes() {
        int counter = 0;
        for (int i = 1; i <= SlotsDonation.MAX_NODES; i++) {
            if (this.bmWaitSts[i]) {
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
        for (int i = 1; i < list.length; i++) {
            list[i] = false;
        }
    }

    private void cleanNodesLists() {
        for (int i = 1; i <= SlotsDonation.MAX_NODES; i++) {
            this.initializedNodes[i] = false;
            this.activeNodes[i] = false;
            this.donorsNodes[i] = false;
            this.bmPendingNodes[i] = false;
        }
    }

    private void cleanCounterGotFirstAt() {
        for (int i = 0; i < SlotsDonation.MAX_NODES - 1; i++) {
            this.counterGotFirstSlotAt[i] = 0;
        }
    }

    private void cleanSlotsLists() {
        for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            this.slotsTable[i] = new Slot();
        }
    }

    private Slot[] cloneSlotTable(Slot[] table) {
        Slot[] cloneTable = new Slot[SlotsDonation.TOTAL_SLOTS];
        for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            cloneTable[i] = new Slot(table[i]);
        }
        return cloneTable;
    }

    private boolean[] cloneBitmapTable(boolean[] table) {
        boolean[] cloneTable;
        cloneTable = Arrays.copyOf(table, SlotsDonation.MAX_NODES + 1);
        return cloneTable;
    }

    public boolean tryFork() {

        /* requesting 1 slot instead of calculate the leftover needed */

        this.println("I need a slot");

        this.mbrRqstSlots();

        if (this.getFreeSlots() == 0) {
            //this.println("No frees slot :(");
            this.counterForksFailed++;
            return (false);
        } else {
            //println("Free slot found, forking");
            this.markSlotUsed();
            this.counterForksSucceded++;
            return (true);
        }


    }

    private int getFirstFree() {
        for (int i = 0; i < SlotsDonation.TOTAL_SLOTS; i++) {
            if (slotsTable[i].isFree()) {
                return i;
            }
        }
        return -1;
    }

    private void updateTable(int slotIndex, int newOwner) {
        slotsTable[slotIndex].setOwner(newOwner);
    }

    public void doExit(int s) {
        //println("Executing exit of a process");
        if (slotsTable[s].isUsed() && slotsTable[s].getOwner() == this.nodeId) {
//            todo: averiguar quien es el nodo spread o como digo que el slot no pertenece a nadie
            this.updateTableOwner(s, 0);
        } else {
            this.println("ERROR: this slot is not owned by node or is not used: " + s);
        }
        this.counterExits++;
    }

    private void updateTableOwner(int slotIndex, int owner) {
//      update the owner and set free the slot
        slotsTable[slotIndex].setOwner(owner);
        slotsTable[slotIndex].setStatus(Slot.STATUS_FREE);
        SlotsMessageTable table = new SlotsMessageTable(this.nodeId, slotsTable);
//        finally broadcast the message
        this.broadcast(table);
    }

    public int[] getCounterGotFirstAt() {
        return this.counterGotFirstSlotAt;
    }

    private int getNextDeltaFork() {
        double val, next_float;

//        do {
//            val =  this.random.nextGaussian() * 50;
//            /*
//            (-50;50) el 70% de las veces (std dev 1)
//            (-100;100) el 95% de las veces
//            */
//            next_float = val + this.arrivalMedian; // median correction
//        } while (next_float < 0 || next_float > 100); // limits

        val = this.random.nextGaussian() * FI_RANGE;
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


        for (int i = 1 + LT_MIN; i < LT_MAX; i++) {
            if (val < 1 - (1 / (double) i)) {
                return (i - 1) * LT_UNIT;
            }
        }

        return (LT_MAX * LT_UNIT);
    }

    /**
     * Returns a uniform random median in the interval [10;90]
     *
     * @return
     */
    private int getNextArrivalMedian() {
        int median = FI_MIN_AVG + this.random.nextInt(FI_MAX_AVG - FI_MIN_AVG + 1);
        return (median);
    }

}