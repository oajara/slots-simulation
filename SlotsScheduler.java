
import daj.Scheduler;

public class SlotsScheduler extends Scheduler {

	private int last = -1;

	// --------------------------------------------------------------------------
	// return index of next program for execution (-1, if none)
	// --------------------------------------------------------------------------
        @Override
	public int nextProgram() {
		int n = getNumber();
		boolean reset = false;
                if(isReady(0)) {
                    return -1;
                }                
		do {
			incTime();
			last++;
			if (last == n) {
				last = 0;
				if (reset) return -1;
				reset = true;
			}
		}
		while (!isReady(last));
		return last;
	}
}