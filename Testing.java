import java.util.Random;

class Testing {
    public static void main(String[] args) {
        Testing t = new Testing();
        Random r = new Random();
//        GaussianGenerator gen = new GaussianGenerator()
        for (int i = 0; i < 50; i++) {
            System.out.println("LifeTime: "+t.getLifeTime(r));
        }
    }
    
    public int getLifeTime(Random r) {
        double val = r.nextGaussian() * 15 ;
        int lt = Math.abs((int) Math.round(val)) + 1;
        lt = lt < 64 ? lt : 64;
        return (lt);
    }
    
    public double getLifeTimeD(Random r) {
        double val = r.nextGaussian();
        int lt = Math.abs((int) Math.round(val));
        return (lt);
    }    
}