import daj.GlobalAssertion;
import daj.Program;
import daj.Node;

class NumberOfSlots extends GlobalAssertion {
    
  private Node[] nodes;
  
    
  public String getText() {
    return "Na na na na";
  }
  
  public NumberOfSlots(Node[] nodes) {
      this.nodes = nodes;
  }

  @Override
  public boolean test(Program[] p) {
    int count = 0;
    for (int j = 1; j < this.nodes.length; j++) {
        count = ((ProgNormalNode)(this.nodes[j].getProgram())).getOwnedSlots() + count;
    }
    return count <= SlotsDonation.TOTAL_SLOTS;
  }
}