import daj.GlobalAssertion;
import daj.Program;
import daj.Node;

class NumberOfSlots extends GlobalAssertion {
    
  private Node[] nodes;
  int count = 0;
    
  public String getText() {
    return "TOTAL is "+SlotsDonation.TOTAL_SLOTS + " while sum is "+count;
  }
  
  public NumberOfSlots(Node[] nodes) {
      this.nodes = nodes;
  }

  @Override
  public boolean test(Program[] p) {
      count = 0;
    for (int j = 1; j < this.nodes.length; j++) {
        count = ((ProgNormalNode)(this.nodes[j].getProgram())).getOwnedSlots() + count;
    }
    return count <= SlotsDonation.TOTAL_SLOTS || count == 0;
  }
}