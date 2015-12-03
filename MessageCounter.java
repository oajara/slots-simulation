
public class MessageCounter {
    public static final int INDEX_JOIN = 0;
    public static final int INDEX_LEAVE = 1;
    public static final int INDEX_REQUEST = 2;
    public static final int INDEX_DONATE = 3;
    public static final int INDEX_INITIALIZED = 4;
    public static final int INDEX_PUTSTATUS = 5;
    public static final int INDEX_NEWSTATUS = 6;
    public static final int INDEX_MERGESTATUS = 7;
    
    private int[] counter = new int[8];
    
    public int inc(int type) {
        this.counter[type]++;
        return this.counter[type];
    }
    
    public int get(int type) {
        return this.counter[type];
    }
    
    public void reset(int type) {
        this.counter[type] = 0;
    }
    
    public void reset() {
        this.counter[INDEX_JOIN] = 0;
        this.counter[INDEX_LEAVE] = 0;
        this.counter[INDEX_REQUEST] = 0;
        this.counter[INDEX_DONATE] = 0;
        this.counter[INDEX_INITIALIZED] = 0;
        this.counter[INDEX_PUTSTATUS] = 0;
        this.counter[INDEX_NEWSTATUS] = 0;
        this.counter[INDEX_MERGESTATUS] = 0;
    }
    
    public String asString() {
        return new String("\n\tJoins: "+this.counter[INDEX_JOIN]+"\n\tLeaves: "+this.counter[INDEX_LEAVE]+
                "\n\tRequests: "+this.counter[INDEX_REQUEST]+"\n\tDonates: "+this.counter[INDEX_DONATE]+
                "\n\tInitialized: "+this.counter[INDEX_INITIALIZED]+"\n\tPut Status: "+this.counter[INDEX_PUTSTATUS]+
                "\n\tNew Status: "+this.counter[INDEX_NEWSTATUS]+"\n\tMerge Status: "+this.counter[INDEX_MERGESTATUS]);
    }
    
}
