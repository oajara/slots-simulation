import daj.*;

public class SlotsDonation extends Application {
    
    public static final int WINW = 1200;
    public static final int WINH = 400;
    public static final int NODES = 16;
    public static final int MAX_NODES = 16;
    public static final int TOTAL_SLOTS = 2048;
    
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

    @Override
    public String getText() {
        return "Slots Donation Algorithm\n \n" +
          "TODO: complete this info";
    }

    @Override
    public void construct() {
        
        this.nodes = new Node[NODES+1];
        
        /* create spread node at index zero */
        this.nodes[0] = node(new ProgSpreadNode(), "Spread Node",
                     //Math.abs(random.nextInt()) % WINW, Math.abs(random.nextInt()) % WINH);
                     WINW / 2, (int) Math.round(WINH * 0.9));
        
        /* create normal nodes from index one */
        int gap = (int) Math.round(WINW / (NODES+1));
        for(int i=1; i<=NODES; i++){
             this.nodes[i] = node(new ProgNormalNode(i), String.valueOf(i),
//                     Math.abs(random.nextInt()) % WINW, Math.abs(random.nextInt()) % WINH);
                       i * gap, (int) Math.round(WINH * 0.1));
        } 
        
        /* build links creating a star */
        for(int i=1; i<=NODES; i++){
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
