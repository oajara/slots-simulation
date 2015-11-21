public class Slot {
    int owner;
    int status;
    
    public static final int STATUS_FREE = 0;
    
    public Slot(int owner, int status) {
        this.owner = owner;
        this.status = status;
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
