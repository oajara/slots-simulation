public class ProgNormalNode extends Program {
    public static final int INACTIVE = 0;
    public static final int ACTIVE = 1;
    public static final int READY = 2;
        
    private Random random;
    private int node_id;
    private int status;
    
    public ProgNormalNode(int id, int slots) { 
        this.random = new Random();
        this.status = INACTIVE;
    }
    
    private void println(String str) {
        System.out.println("Node[" + node_id + "]: "+str);
    }    
    
    public void main()  {
        // Join Spread
        out(0).send(new SpreadMessage(SpreadMessage.JOIN_REQ, node_id));

    }
    
    /**
     * Broadcasting a message is sending it to the spread node
     * @param msg 
     */
    public void broadcast(Message msg) {
        out(0).send(msg);
    }
    
    public String getText() {
        return "Node: " + node_id + "\nProcesses: N/A" ;
    }
}