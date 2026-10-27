package mizzen.util;

public class ThrottledRunnable {

    private long lastActivityRunTime = 0;
    private long interval = 5000;

    public ThrottledRunnable(long interval) {
        super();
        this.interval = interval;
    }

    public ThrottledRunnable() {
        this(5000);
    }

    public void run(Runnable activity) {
        long now = System.currentTimeMillis();
        if (now - lastActivityRunTime > interval) {
            activity.run();
            lastActivityRunTime = now;
        }
    }
    

}
