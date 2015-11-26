import java.util.Timer;
import java.util.TimerTask;


public class ProcessManagement {
    Timer timer;
    ProgNormalNode owner;

    public ProcessManagement(int seconds, ProgNormalNode owner) {
        this.owner = owner;
        timer = new Timer();
        timer.schedule(new ForExit(), seconds * 1000, seconds * 1000);
    }

    class ForExit extends TimerTask {
        public void run() {
            println("Time's up!");
        }
    }

    private void println(String str) {
        this.owner.println(str);
    } 

//    public static void main(String args[]) {
//    }
}
