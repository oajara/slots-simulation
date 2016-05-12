import java.util.Arrays;
import java.util.Random;

/*
TODO: generalizar vector shuffle: max 64, pasar actual length como param 
*/

class Testing {
    public static void main(String[] args) {
        Testing t = new Testing();
        Random r = new Random();
//        GaussianGenerator gen = new GaussianGenerator()
//        for (int i = 0; i < 500000; i++) {
//            //System.out.println("LifeTime: "+t.getLifeTime(r));
//            System.out.println(t.getNextForkTime(r));
//        }
        int[] vector = {1,2,3,4,5,6,7,8,9,10};
        int[] shuff = Testing.RandomizeArray(vector);
        System.out.println(Arrays.toString(shuff));
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
    
    public int getNextForkTime(Random r) {
        double val =  r.nextGaussian() * 90;
        int next = (int)val+65;
        while(next < 1) {
            val =  r.nextGaussian() * 90;
            next = (int)val+65;
        }
        
        return (next);   
    }
    
    public static int[] RandomizeArray(int[] array){
            Random rgen = new Random();  // Random number generator			

            for (int i=0; i<array.length; i++) {
                int randomPosition = rgen.nextInt(array.length);
                int temp = array[i];
                array[i] = array[randomPosition];
                array[randomPosition] = temp;
            }

            return array;
    }    
    
//    public int getUnif(Random r) {
//        return r.nextInt(1)
//    }
}