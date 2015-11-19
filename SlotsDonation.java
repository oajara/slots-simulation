import daj.*;
import java.util.*;

public class SlotsDonation extends Application {
    
    public static final int WINW = 800;
    public static final int WINH = 600;
    public static final int NODES = 3;
    public static final int MAX_NODES = 4;
    public static final int SLOTS = 30;

    public static void main(String[] args) {
        new SlotsDonation().run();
    }

    public void resetStatistics() {
    }

    public SlotsDonation() {
        super("Slots Donation Algorithm", WINW, WINH);
    }

    public String getText() {
        return "Slots Donation Algorithm\n \n" +
          "TODO: complete this info";
    }

    /**
     * Create randomly distributed mesh of nodes.
     */
    public void construct() {
        Random random = new Random();
        Node[] nodes;
        
        nodes = new Node[NODES+1];
        
        /* create spread node at index zero */
        nodes[0] = node(new ProgSpreadNode(), "Spread Node",
                     Math.abs(random.nextInt()) % WINW, Math.abs(random.nextInt()) % WINH);
        
        /* create normal nodes from index one */
        for(int i=1; i<=NODES; i++){
             nodes[i] = node(new ProgNormalNode(i, SLOTS/NODES), String.valueOf(i),
                     Math.abs(random.nextInt()) % WINW, Math.abs(random.nextInt()) % WINH);
        } 
        
        /* build links creating a mesh */
        for(int i=0; i<=NODES; i++){
            for(int j=0; j<=NODES; j++){
                if(i != j) {
                    link(nodes[i], nodes[j]);
                }
            }
        }          
    }
    
}
//
//class DonationMessage extends Message {
//    public static final int DONATE = 1;
//    public static final int REQUEST = 2;
//    int type;
//    
//    public DonationMessage(int type) {
//        this.type = type;
//    }
//    
//    public String getText() {
//        switch(this.type) {
//            case DONATE:
//                return "Donate";
//            case REQUEST:
//                return "Request";
//            default:
//                return "Unknown";
//        }
//    }
//}

/**
 * Message for Slot Donation
 */
//class SlotDonateMsg extends Message {
//    int donor;
//    int quantity;
//    
//    
//    public SlotDonateMsg(int donor, int quantity) {
//        this.donor = donor;
//        this.quantity = quantity;
//    }
//    
//    public int getDonor() {
//        return this.donor;
//    }
//    
//    public int getQuantity() {
//        return this.quantity;
//    }
//    
//    public String getText() {
//        return String.valueOf(this.donor) + " donates " +
//                String.valueOf(this.quantity) + " slots ";
//    }
//}
//
///**
// * Message for Slot Requests
// */
//class SlotRequestMsg extends Message {
//    int request;
//    int quantity;
//    
//    
//    public SlotRequestMsg(int request, int quantity) {
//        this.request = request;
//        this.quantity = quantity;
//    }
//    
//    public int getDonor() {
//        return this.request;
//    }
//    
//    public int getQuantity() {
//        return this.quantity;
//    }
//    
//    public String getText() {
//        return String.valueOf(this.request) + " requests " +
//                String.valueOf(this.quantity) + " slots ";
//    }
//}

