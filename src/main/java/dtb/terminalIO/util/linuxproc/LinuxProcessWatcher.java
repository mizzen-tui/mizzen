package dtb.terminalIO.util.linuxproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import dtb.terminalIO.util.ConsumeResult;
import dtb.terminalIO.util.linuxproc.ExecUtil.StreamLineGobbler;

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
    Thread controllerThread;
    String name;
    boolean controlledInterrupt = true;

    Supplier<Boolean> isActive = new Supplier<Boolean>() {
        @Override
        public Boolean get() {
            return isAlive();
        }};

    public LinuxProcessWatcher setInvokeScript(String invokeScript) {
        this.invokeScript = invokeScript;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public synchronized void invokeLongRunning(ConsumeResult<String> consumeOutput, ConsumeResult<Integer> consumeResult) throws Exception {
        if (invokeScript == null) {
            throw new NullPointerException("invokeScript not set.  Please call setInvokeScript() first");
        }

        if (invoking.get()) {
            throw new IllegalStateException("Already invoked and still running!");
        }

        controlledInterrupt = false;
        
        invoking.set(true);

        Runnable invoker = new Runnable() {
            @Override
            public void run() {
                try {

                    String[] commands = new String[] { "/bin/sh", "-c",LinuxProcessWatcher.this.invokeScript};

                    ProcessBuilder procBuilder = new ProcessBuilder(commands);

                    procBuilder.redirectErrorStream(true);
                    
                    LinuxProcessWatcher.this.process = procBuilder.start();
                    
                    String gobblerName = "outputGobbler";
                    if ( LinuxProcessWatcher.this.name != null) {
                        gobblerName = gobblerName + "-" + name;
                    }
                    outputGobbler = new Thread(new StreamLineGobbler(LinuxProcessWatcher.this.process.getInputStream(), consumeOutput, isActive),gobblerName);

                    outputGobbler.start();

                    LinuxProcessWatcher.this.lastResult = LinuxProcessWatcher.this.process.waitFor();

                    if (outputGobbler != null) {
                        outputGobbler.join();
                    }

                    consumeResult.accept(LinuxProcessWatcher.this.lastResult);

                } catch (Exception e) {
                    if (controlledInterrupt && e instanceof InterruptedException) {
                        invoking.set(false);
                        process = null;
                        outputGobbler = null;
                        return;
                    }
                    throw new RuntimeException(e);
                }

                invoking.set(false);
                process = null;
                outputGobbler = null;

            }

        };

        String threadName = "LinuxProcessWatcher-" + (this.name != null?(name+"-"):"") + nextThreadNum(); 

        controllerThread = new Thread(invoker, threadName);
        controllerThread.start();
    }
    
    private synchronized void killGobblerThread() {
        if (outputGobbler != null && outputGobbler.isAlive()) {
            if (outputGobbler != null && outputGobbler.isAlive()) {
                controlledInterrupt = true;
                outputGobbler.interrupt();
            }
        }
        outputGobbler = null;
    }

    private synchronized void killControllerThread() {
        if (controllerThread != null && controllerThread.isAlive()) {
            if (controllerThread != null && controllerThread.isAlive()) {
                controlledInterrupt = true;
                controllerThread.interrupt();
            }
        }
        controllerThread = null;
    }
    
    public synchronized void stopRequest() {
        invoking.set(false);
        Process theProcess = process;
        if (process != null) {
            if (outputGobbler != null && outputGobbler.isAlive()) {
                if (theProcess.isAlive()) {
                    try {
                        theProcess.getInputStream().close();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    theProcess.destroy();
                }
            }
        }
        killGobblerThread();
        killControllerThread();
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

    public static LinuxProcessWatcher tailFile(String name, String fileName) throws Exception {

        File fileToTail = new File(fileName);

        if (!fileToTail.exists()) {
            throw new FileNotFoundException(fileName + " not found.  Cannot perform tail.");
        }

        String tailCMD = "/usr/bin/tail -f " + fileToTail.getAbsolutePath();

        LinuxProcessWatcher watcher = new LinuxProcessWatcher();
        
        if (name != null) {
            watcher.setName(name);
        }

        watcher.setInvokeScript(tailCMD);

        return watcher;

    }

    public static LinuxProcessWatcher tailFile(String fileName) throws Exception {
        return tailFile(null, fileName);
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