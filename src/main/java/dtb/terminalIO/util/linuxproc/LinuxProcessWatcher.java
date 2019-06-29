package dtb.terminalIO.util.linuxproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import dtb.terminalIO.util.ConsumeResult;
import dtb.terminalIO.util.linuxproc.ExecUtil.StreamLineGlobber;

public class LinuxProcessWatcher {

    /* For autonumbering LinuxProcessWatcher threads. */
    private static int threadInitNumber;

    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    AtomicBoolean invoking = new AtomicBoolean(false);
    String invokeScript;
    int lastResult = -123;
    Process process = null;
    Thread outputGobbler;

    Supplier<Boolean> isActive = new Supplier<Boolean>() {
        @Override
        public Boolean get() {
            return isAlive();
        }};

    public LinuxProcessWatcher setInvokeScript(String invokeScript) {
        this.invokeScript = invokeScript;
        return this;
    }

    public synchronized void invokeLongRunning(ConsumeResult<String> consumeOutput, ConsumeResult<Integer> consumeResult) throws Exception {
        if (invokeScript == null) {
            throw new NullPointerException("invokeScript not set.  Please call setInvokeScript(() first");
        }

        if (invoking.get()) {
            throw new IllegalStateException("Already invoked and still running!");
        }

        invoking.set(true);

        Runnable invoker = new Runnable() {
            @Override
            public void run() {
                try {

                    String[] commands = new String[] { "/bin/sh", "-c",LinuxProcessWatcher.this.invokeScript};

                    ProcessBuilder procBuilder = new ProcessBuilder(commands);

                    procBuilder.redirectErrorStream(true);
                    
                    LinuxProcessWatcher.this.process = procBuilder.start();
                    
                    outputGobbler = new Thread(new StreamLineGlobber(LinuxProcessWatcher.this.process.getInputStream(), consumeOutput, isActive),"outputGobbler");

                    outputGobbler.start();

                    LinuxProcessWatcher.this.lastResult = LinuxProcessWatcher.this.process.waitFor();

                    outputGobbler.join();

                    consumeResult.accept(LinuxProcessWatcher.this.lastResult);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                invoking.set(false);
                process = null;
                outputGobbler = null;

            }

        };

        new Thread(invoker, "LinuxProcessWatcher-" + nextThreadNum()).start();
    }
    
    public synchronized void stopRequest() {
        invoking.set(false);
        if (process != null) {
            if (outputGobbler != null && outputGobbler.isAlive()) {
                if (process.isAlive()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    process.destroy();
                }
            }
        }
    }

    public int getLastResult() {
        return lastResult;
    }

    public boolean isAlive() {
        return invoking.get();
    }

    public synchronized String invoke() throws Exception {
        if (invoking.get()) {
            throw new IllegalStateException("Already invoked!");
        }

        if (invokeScript == null) {
            throw new NullPointerException("invokeScript not set.  Please call setInvokeScript() first");
        }

        invoking.set(true);
        String result = ExecUtil.executeWithShell(this.invokeScript);
        invoking.set(false);
        return result;
    }

    public static LinuxProcessWatcher tailFile(String fileName) throws Exception {

        File fileToTail = new File(fileName);

        if (!fileToTail.exists()) {
            throw new FileNotFoundException(fileName + " not found.  Cannot perform tail.");
        }

        String tailCMD = "/usr/bin/tail -f " + fileToTail.getAbsolutePath();

        LinuxProcessWatcher watcher = new LinuxProcessWatcher();

        watcher.setInvokeScript(tailCMD);

        return watcher;

    }

    public static LinuxProcessWatcher tailFile(String fileName, ConsumeResult<String> consumeOutput) throws Exception {

        LinuxProcessWatcher watcher = tailFile(fileName);
        watcher.invokeLongRunning(consumeOutput, (result)->{});

        return watcher;

    }

    public static LinuxProcessWatcher viewTextFile(String fileName) throws Exception {

        File fileToTail = new File(fileName);

        if (!fileToTail.exists()) {
            throw new FileNotFoundException(fileName + " not found.  Cannot perform tail.");
        }

        // todo: Should not use cat !!!-- should just read the damn file!!!!  yeesh..

        String tailCMD = "cat " + fileToTail.getAbsolutePath();

        LinuxProcessWatcher watcher = new LinuxProcessWatcher();

        watcher.setInvokeScript(tailCMD);

        return watcher;

    }

    public static LinuxProcessWatcher viewTextFile(String fileName, ConsumeResult<String> consumeOutput) throws Exception {

        LinuxProcessWatcher watcher = viewTextFile(fileName);
        watcher.invokeLongRunning(consumeOutput, (result)->{});

        return watcher;

    }

}