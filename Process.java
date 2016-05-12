import java.util.Random;


public class Process {
    private static final int MIN_LIFETIME = 2;
    private static final int MAX_LIFETIME = 64;

    int time_left;

    public Process(Random r) {
        this.time_left = this.getLifeTime(r);
    }
    
    private int getLifeTime(Random r) {
        double val = r.nextGaussian() * 15 ;
        int lt = Math.abs((int) Math.round(val)) + Process.MIN_LIFETIME;
        lt = lt < Process.MAX_LIFETIME ? lt : Process.MAX_LIFETIME;
        return (lt);
    }
}