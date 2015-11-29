public class Slot {
    int owner;
    int status;
    
    public static final int STATUS_FREE = 0;
    public static final int STATUS_USED = 1;
    public static final int STATUS_DONATING = 2;
    
    public Slot(int owner, int status) {
        this.owner = owner;
        this.status = status;
    }
    
    public Slot(Slot originalSlot) {
        this.owner = originalSlot.getOwner();
        this.status = originalSlot.getStatus();
    }
    
    public boolean isFree() {
        return (this.status == STATUS_FREE);
    }
    
    public boolean isUsed() {
        return (this.status == STATUS_USED);
    }    

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    

}
