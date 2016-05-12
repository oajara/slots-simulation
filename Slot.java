public class Slot {
    int owner;
    int status;
    int processTimeLeft;
    
    public static final int STATUS_FREE = 0;
    public static final int STATUS_USED = 1;
    public static final int STATUS_DONATING = 2;
    public static final int STATUS_UNINIT = 3;
    
    public Slot(int owner, int status, int timeLeft) {
        this.owner = owner;
        this.status = status;
        this.processTimeLeft = timeLeft;
    }
    
    public Slot() {
        this.owner = 0;
        this.status = STATUS_UNINIT;
        this.processTimeLeft = -1;
    }    
    
    public Slot(Slot originalSlot) {
        this.owner = originalSlot.getOwner();
        this.status = originalSlot.getStatus();
        this.processTimeLeft = originalSlot.getProcessTimeLeft();
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
    
    public void setProcessLifetime(int timeUnits) {
        this.processTimeLeft = timeUnits;
    }

    public int getStatus() {
        return status;
    }
    
    public int getProcessTimeLeft() {
        return processTimeLeft;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    

}
