
public class SlotsMessageRequest extends SlotsMessage {
    int need_slots;
    int free_slots;
    int owned_slots;

    public SlotsMessageRequest(int need_slots, int free_slots, int owned_slots, int senderId) {
        super(senderId);
        this.need_slots = need_slots;
        this.free_slots = free_slots;
        this.owned_slots = owned_slots;
    }

    public int getNeedSlots() {
        return need_slots;
    }

    public int getFreeSlots() {
        return free_slots;
    }

    public int getOwnedSlots() {
        return owned_slots;
    }

    @Override
    public String getText() {
        return "Sender: "+this.getSenderId()+"\nRequest Message\nNeed:"+this.need_slots+"\nFree:"+
                this.free_slots+"\nOwned:"+this.owned_slots+"\n";
    }
}

