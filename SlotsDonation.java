//package daj;

import daj.*;


public class SlotsDonation extends Application {

    public static final int WINW = 600;
    public static final int WINH = 300;
    public static final int NODES = 8;
    public static final int MAX_NODES = NODES;
    public static final int TOTAL_SLOTS = 128;

    private Node[] nodes;

    public static void main(String[] args) {
        new SlotsDonation().run();
    }

    @Override
    public void resetStatistics() {
    }

    public SlotsDonation() {
        super("Slots Donation Algorithm", WINW, WINH);
    }

    public void writeInfo() {
        System.out.println("Run Params: \nNR_NODES: " + NODES + "\nSLOTS: " + TOTAL_SLOTS +
                "\nMIN_OWNED_SLOTS: " + ProgNormalNode.MIN_OWNED_SLOTS + "\nFREE_SLOTS_LOW: " +
                ProgNormalNode.FREE_SLOTS_LOW + "\nSLOTS_BY_MSG: " + ProgNormalNode.SLOTS_BY_MSG);
        System.out.println("Joins, Leaves, Requests, Donates, Inits, PutStatus, NewStatus, MergeStatus");
        ((ProgSpreadNode) (this.nodes[0].getProgram())).getInfoLine();
        System.out.println("NodeId, ForksOK, ForksFail, Exits, TotalRequested, TotalReceived, GotZero");
        for (int i = 1; i < this.nodes.length; i++) {
            ((ProgNormalNode) (this.nodes[i].getProgram())).getInfoLine();
        }
    }

    @Override
    public String getText() {
        this.writeInfo();
        return "Slots Donation Algorithm\n \n" +
                "TODO: complete this info";
    }

    public void printParameters() {
        System.out.println("NR_NODES: " + NODES + "\nSLOTS: " + TOTAL_SLOTS +
                "\nMIN_OWNED_SLOTS: " + ProgNormalNode.MIN_OWNED_SLOTS + "\nFREE_SLOTS_LOW: " +
                ProgNormalNode.FREE_SLOTS_LOW + "\nSLOTS_BY_MSG: " + ProgNormalNode.SLOTS_BY_MSG +
                "\nMEDIAN_CHANGE_INTERVAL: " + ProgNormalNode.MEDIAN_CHANGE_INTERVAL + "\nLT_UNIT: " +
                ProgNormalNode.LT_UNIT + "\nLT_MIN: " + ProgNormalNode.LT_MIN +
                "\nLT_MIN: " + ProgNormalNode.LT_MAX + "\nFI_MAX: " +
                ProgNormalNode.FI_MAX + "\nFI_MIN: " + ProgNormalNode.FI_MIN +
                "\nFI_RANGE: " + ProgNormalNode.FI_RANGE + "\nFI_MIN_AVG: " +
                ProgNormalNode.FI_MIN_AVG + "\nFI_MAX_AVG: " + ProgNormalNode.FI_MAX_AVG);
    }

    @Override
    public void construct() {
        this.printParameters();
        this.nodes = new Node[NODES + 1];
        
        /* create spread node at index zero */
        this.nodes[0] = node(new ProgSpreadNode(), "Spread Node",
                //Math.abs(random.nextInt()) % WINW, Math.abs(random.nextInt()) % WINH);
                WINW / 2, (int) Math.round(WINH * 0.9));

        //assign nodes list to spread node
        ((ProgSpreadNode) (this.nodes[0].getProgram())).setNodeList(nodes);
        
        /* create normal nodes from index one */
        int gap = (int) Math.round(WINW / (NODES + 1));
        for (int i = 1; i <= NODES; i++) {
            this.nodes[i] = node(new ProgNormalNode(i), String.valueOf(i),
//                     Math.abs(random.nextInt()) % WINW, Math.abs(random.nextInt()) % WINH);
                    i * gap, (int) Math.round(WINH * 0.1));
        } 
        
        /* build links creating a star */
        for (int i = 1; i <= NODES; i++) {
            link(this.nodes[0], this.nodes[i]);
            link(this.nodes[i], this.nodes[0]);
        }
        
        /* assertion object */
//        for(int i=1; i<=NODES; i++){
//             ((ProgNormalNode)this.nodes[i].getProgram()).setSlotsAssertion(new NumberOfSlots(this.nodes));
//        }

//        this.setScheduler(new SlotsScheduler());

    }

}
