import java.util.Arrays;
public class SlotsMessageDonate extends SlotsMessage {
    int requester;
    int[] donatedIdList;

    public SlotsMessageDonate(int requester, int[] donatedIdList, int senderId) {
        super(senderId);
        this.requester = requester;
        this.donatedIdList = donatedIdList;
    }

    public int getRequester() {
        return requester;
    }

    
    public int[] getDonatedIdList() {
        return donatedIdList;
    }    
    
    @Override
    public String getText() {
        return "Donation Message from Node#"+requester+" to Node#"+senderId+ "\nDonated Slots IDs: "+Arrays.toString(donatedIdList);
    }
}


