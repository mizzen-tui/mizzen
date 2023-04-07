package dtb.terminalIO.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

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

    public void reset() {
        this.start = System.currentTimeMillis();
        elapsed = -1;
    }
    
    public long recalcElapsed() {
        elapsed = System.currentTimeMillis() - start;
        return elapsed;
    }
    
    public static ElapsedTime start() {
        return new ElapsedTime();
    }

    public static ElapsedTime onRunOf(Runnable activity) {
        ElapsedTime et = start();
        activity.run();
        et.elapsed();
        return et;
    }
    
    public static <T> T getAndTime(Supplier<T> calc, Consumer<Long> elapseTimeConsumer) {
        ElapsedTime et = start();
        T result = calc.get();
        elapseTimeConsumer.accept(et.elapsed());
        return result;
    }

    
    
    public String toString() {
        return String.valueOf(elapsed());
    }
    
    public static class Average {
        
        private String      name;
        private int         counter = 0;
        private long        totalElapsed = 0;
        
        private ElapsedTime elapsed;

        public Average(String name) {
            super();
            this.name = name;
        }

        public void startTimer() {
            elapsed = new ElapsedTime();
            this.counter++;
        }

        public void stopTimer() {
            long tElapsed = elapsed.elapsed();
            this.totalElapsed += tElapsed;
        }

        public long average() {
            return counter==0? 0 : totalElapsed / counter;
        }
        
        public String toString() {
            return name + ": " + (elapsed == null ? "not started" : elapsed.toString() + "  avg:" + average());
        }
    }

}
