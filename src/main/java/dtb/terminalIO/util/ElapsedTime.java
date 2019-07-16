package dtb.terminalIO.util;

public class ElapsedTime {

    long start;
    long elapsed = -1;

    public ElapsedTime() {
        this.start = System.currentTimeMillis();
    }
    
    public long elapsed() {
        if (elapsed == -1) {
            elapsed = System.currentTimeMillis() - start;
        }
        return elapsed;
    }

    public long recalcElapsed() {
        elapsed = System.currentTimeMillis() - start;
        return elapsed;
    }
    
    public static ElapsedTime start() {
        return new ElapsedTime();
    }

    public static ElapsedTime onRun(Runnable activity) {
        ElapsedTime et = start();
        activity.run();
        et.elapsed();
        return et;
    }

    
    
    public String toString() {
        return String.valueOf(elapsed());
    }

}
