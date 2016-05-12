import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class ProcessManagement {
    Timer timer;
    ProgNormalNode owner;
    private Random random;

    public ProcessManagement(int seconds, ProgNormalNode owner) {
        this.random = new Random();
        this.owner = owner;
        timer = new Timer();
        timer.schedule(new ForExit(), seconds * 1000, seconds * 1000);
    }

    class ForExit extends TimerTask {
        int action;
        public void run() {
            if(owner.isInitialized()) {
                action = random.nextInt(2);
                if (action == 0) {
                    owner.tryFork();
                } else if (owner.getUsedSlots() > 0) {
//                    owner.doExit();
                }
            } else {
                println("Node not initialized, won't mess with processes");
            }
        }
    }

    private void println(String str) {
        this.owner.println(str);
    } 

//    public static void main(String args[]) {
//    }
}
