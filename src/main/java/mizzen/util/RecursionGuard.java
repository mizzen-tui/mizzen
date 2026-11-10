
package mizzen.util;

import java.util.function.Supplier;

/**
 * RecursionGuard
 * 
 * <p/>
 * 
 * Prevents a piece of code from being reentrant or recursively called
 */
public class RecursionGuard {

    private boolean executing = false;

    /**
     * Executes the supplier unless this guard is already executing.
     * Returns null if a recursive call is suppressed.
     */
    public <T> T execute(Supplier<T> supplier) {
        if (executing) {
            return null;
        }

        executing = true;
        try {
            return supplier.get();
        } finally {
            executing = false;
        }
    }

    /**
     * Runnable convenience overload.
     */
    public void execute(Runnable action) {
        if (executing) {
            return;
        }

        executing = true;
        try {
            action.run();
        } finally {
            executing = false;
        }
    }
}
