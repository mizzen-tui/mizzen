package mizzen.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/***
 * A queue wrapper that wraps a queue that can be drained
 *
 * @param <T>
 */
public class DrainableBlockingQueue<T> {
    
    private static final int EVENT_Q_CAPACITY = 512;
    private BlockingQueue<T> queue = null;
    
    
    public DrainableBlockingQueue(int size) {
        queue = new ArrayBlockingQueue<T>(size);
    }

    public DrainableBlockingQueue() {
        this(EVENT_Q_CAPACITY);
    }
    
    
    public void submitToQ(T t) {
        try {
            queue.put(t);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public int drainQWithTimeLimit(int millisec, Consumer<T>  consume) {
        if (queue == null) {
            return 0;
        }
        int count = 0;
        long start = System.currentTimeMillis();
        T s = queue.poll();
        while (s !=null) {
            consume.accept(s);
            count++;
            if ((System.currentTimeMillis() - start) >= millisec) {
                break;
            }
            s = queue.poll();
        }
        return count;
    }
    
}
