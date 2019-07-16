package dtb.terminalIO.util.linuxproc;

import java.util.function.Consumer;

import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.util.linuxproc.ExecUtil;

public class PIDMonitor {

    private String            pidScript;
    private RepeatingInterval monitor = null;

    public PIDMonitor(String pidScript) {
        super();
        this.pidScript = pidScript;
    }

    public int getPID() throws Exception {
        String result = ExecUtil.executeWithShell(pidScript);
        try {
            if (result != null) {
                result = result.trim();
            }
            int pid = Integer.parseInt(result);
            return pid;
        } catch (NumberFormatException e) {
            if (result == null || result.trim().length() == 0) {
                return -1;
            }
            throw new IllegalStateException("Failed to get PID.  Instead got: " + result);
        }
    }

    public boolean isRunning() throws Exception {
        return getPID() > 0;
    }

    private OnIdleEventEventHandler.RepeatingInterval makeMonitor(Consumer<Integer> success, Consumer<Exception> failure) {
        return new OnIdleEventEventHandler.RepeatingInterval(2000, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                try {
                    int pid = getPID();
                    success.accept(pid);
                } catch (Exception e) {
                    failure.accept(e);
                }
            }
        });
    }

    public synchronized void activatePIDMonitorUsingEventBus(EventBus eventBus, Consumer<Integer> success, Consumer<Exception> failure) {

        if (monitor != null) {
            eventBus.removeEventHandler(monitor);
            monitor = null;
        }

        monitor = makeMonitor(success, failure);

        eventBus.appendEventHandler(monitor);
    }

}
