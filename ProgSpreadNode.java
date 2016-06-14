import daj.Message;
import daj.Program;
import daj.Node;
import java.util.Arrays;
import java.util.Random;

/*
TODO: generalizar vector shuffle: max 64, pasar actual length como param
*/

public class ProgSpreadNode extends Program {
    int broadcasted = 0;
    private final boolean[] registeredNodes = new boolean[SlotsDonation.MAX_NODES+1];
    private final MessageCounter[] counters = new MessageCounter[SlotsDonation.MAX_NODES+1];
    private Node[] nodeList;

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

    public void setNodeList(Node[] nodeList) {
        this.nodeList = nodeList;
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
        } else if (msg instanceof SlotsMessageTable) {
            println("Receive " + msg.getClass().toString() + " from Node#" + ((SlotsMessage) msg).getSenderId());
            handleUpdateTable((SlotsMessageTable) msg);
        } else {
            println("Receive "+ msg.getClass().toString() +" from Node#"+ ((SlotsMessage)msg).getSenderId());
            handleSlotsMessage((SlotsMessage)msg);
        }
    }

    private void handleUpdateTable(SlotsMessageTable msg){
        sendToAll(msg);
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
        int[] vector = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};
        int[] shuff = Testing.RandomizeArray(vector);

        for(int i=0; i<SlotsDonation.NODES; i++){
            if(this.isActive(shuff[i])) {
                //this.println("... to node#"+i);
                out(shuff[i]-1).send(msg); // because link to node n is locate at out(n-1)
            }
        }
        broadcasted++;
    }


    public void getInfoLine() {
        int[] counterGotFirstSlotAt = new int[SlotsDonation.MAX_NODES-1];
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



        for(int n = 1; n <= SlotsDonation.MAX_NODES; n++) {
            for(int c = 0; c < SlotsDonation.MAX_NODES-1; c++) {
                counterGotFirstSlotAt[c] += ((ProgNormalNode)(this.nodeList[n].getProgram())).getCounterGotFirstAt()[c];
            }
        }

        System.out.println("At Message,Counter");
        for(int c = 0; c < SlotsDonation.MAX_NODES-1; c++) {
            System.out.println(""+(c+1)+","+ counterGotFirstSlotAt[c]);
        }

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

    public int[] randomizeArray(int[] array){
        Random rgen = new Random();  // Random number generator

        for (int i=0; i<array.length; i++) {
            int randomPosition = rgen.nextInt(array.length);
            int temp = array[i];
            array[i] = array[randomPosition];
            array[randomPosition] = temp;
        }

        return array;
    }

}