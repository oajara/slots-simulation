import daj.*;
import java.util.*;

public class SlotsDonation extends Application {
    
    public static final int WINW = 800;
    public static final int WINH = 600;
    public static final int NODES = 2;
    public static final int MAX_NODES = 4;
    public static final int TOTAL_SLOTS = 16;

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

    /**
     * Create randomly distributed mesh of nodes.
     */
    @Override
    public void construct() {
        Node[] nodes;
        
        nodes = new Node[NODES+1];
        
        /* create spread node at index zero */
        nodes[0] = node(new ProgSpreadNode(), "Spread Node",
                     //Math.abs(random.nextInt()) % WINW, Math.abs(random.nextInt()) % WINH);
                     WINW / 2, (int) Math.round(WINH * 0.9));
        
        /* create normal nodes from index one */
        int gap = (int) Math.round(WINW / (NODES+1));
        for(int i=1; i<=NODES; i++){
             nodes[i] = node(new ProgNormalNode(i), String.valueOf(i),
//                     Math.abs(random.nextInt()) % WINW, Math.abs(random.nextInt()) % WINH);
                       i * gap, (int) Math.round(WINH * 0.1));
        } 
        
        /* build links creating a star */
        for(int i=1; i<=NODES; i++){
            link(nodes[0], nodes[i]);
            link(nodes[i], nodes[0]);
        }          
        
//        link(nodes[1], nodes[2]);
//        link(nodes[2], nodes[1]);
    }
    
}
