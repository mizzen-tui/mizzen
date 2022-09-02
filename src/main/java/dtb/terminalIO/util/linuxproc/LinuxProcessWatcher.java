package dtb.terminalIO.util.linuxproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import dtb.terminalIO.util.ConsumeResultAction;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;
import dtb.terminalIO.util.linuxproc.ExecUtil.StreamLineGobbler;

public class LinuxProcessWatcher {

    /* For autonumbering LinuxProcessWatcher threads. */
    private static int threadInitNumber;

    private static synchronized int nextThreadNum() {
        threadInitNumber++;
        return threadInitNumber;
    }

    private static synchronized int currentThreadNum() {
        return threadInitNumber;
    }

    AtomicBoolean invoking = new AtomicBoolean(false);
    String invokeScript;
    String pidScript;
    int lastResult = -123;
    Process process = null;
    Thread outputGobbler;
    Thread controllerThread;
    String name;
    volatile boolean controlledInterrupt = true;
    String workingDirectory;
    Map<String, String> environment;

    Supplier<Boolean> isActive = new Supplier<Boolean>() {
        @Override
        public Boolean get() {
            return isAlive();
        }
    };

    public LinuxProcessWatcher setInvokeScript(String invokeScript) {
        this.invokeScript = invokeScript;
        return this;
    }

    public LinuxProcessWatcher setPIDScript(String pidScript) {
        this.pidScript = pidScript;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public LinuxProcessWatcher setWorkingDirectory(String workingDirectory) {
        if (workingDirectory != null) {
            // validate ..
            if ( ! new File(workingDirectory).exists()) {
                throw new IllegalArgumentException("specified working directory '"+ workingDirectory + "' not found");
            }
        }
        this.workingDirectory = workingDirectory;
        return this;
    }

    public LinuxProcessWatcher setEnvironment(Map<String, String> environment) {
        this.environment = environment;
        return this;
    }

    public synchronized void invokeLongRunning(TerminalStreamOutputParser parser, ConsumeResultAction<Integer> onCompletedAction, ConsumeResultAction<Exception> onExceptionAction)
            throws Exception {

        boolean readAsych = true;
        TerminalStreamReader terminalReader = new TerminalStreamReader(parser, (in) -> isActive.get(), readAsych);
        Function<InputStream, Thread> gobblerThreadBuilder = (in) -> new Thread(terminalReader.asRunnable(in));
        invokeLongRunning(gobblerThreadBuilder, onCompletedAction, onExceptionAction);
    }

    public synchronized void invokeLongRunning(ConsumeResultAction<String> consumeStdOutputAction,
                                               ConsumeResultAction<Integer> onCompletedAction) throws Exception {
        Function<InputStream, Thread> gobblerThreadBuilder = (in) -> new Thread(new StreamLineGobbler(in, consumeStdOutputAction, isActive));
        invokeLongRunning(gobblerThreadBuilder, onCompletedAction);
    }

    public synchronized void invokeLongRunning(Function<InputStream, Thread> gobblerThreadBuilder,
            ConsumeResultAction<Integer> consumeResult) throws Exception {
        invokeLongRunning(gobblerThreadBuilder,  consumeResult, null);
    }

    private static int outputGobblerThreadCounter = 0;
    
    public synchronized void invokeLongRunning(Function<InputStream, Thread> gobblerThreadBuilder,
                                               ConsumeResultAction<Integer> consumeResult, 
                                               ConsumeResultAction<Exception> consumeException) throws Exception {
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

                    String[] commands = new String[] { ExecUtil.SSH_Path(), "-c", LinuxProcessWatcher.this.invokeScript };

                    ProcessBuilder procBuilder = new ProcessBuilder(commands);
                    
                    if (LinuxProcessWatcher.this.workingDirectory != null) {
                        procBuilder.directory(new File(LinuxProcessWatcher.this.workingDirectory ));
                    }

                    if (LinuxProcessWatcher.this.environment != null) {
                        Map<String, String> procENV = procBuilder.environment();
                        for (String envVarName : LinuxProcessWatcher.this.environment.keySet()) {
                            String value = LinuxProcessWatcher.this.environment.get(envVarName);
                            if (value != null && value.trim().length()!=0) {
                                procENV.put(envVarName, value);
                             }
                        }
                    }

                    procBuilder.redirectErrorStream(true);

                    LinuxProcessWatcher.this.process = procBuilder.start();
                    
                    outputGobbler = gobblerThreadBuilder.apply(LinuxProcessWatcher.this.process.getInputStream()); // new
                                                                                                                   // Thread(new
                                                                                                                   // StreamLineGobbler(LinuxProcessWatcher.this.process.getInputStream(),
                                                                                                                   // consumeOutput,
                                                                                                                   // isActive),gobblerName);

                    String gobblerName = "LinuxProcessWatcher-gobbler-" + (LinuxProcessWatcher.this.name != null ? (LinuxProcessWatcher.this.name + "-") : "") + currentThreadNum();
                    outputGobbler.setName(gobblerName);

                    outputGobbler.start();

                    LinuxProcessWatcher.this.lastResult = LinuxProcessWatcher.this.process.waitFor();

                    if (outputGobbler != null) {
                        outputGobbler.join();
                    }

                    consumeResult.accept(LinuxProcessWatcher.this.lastResult);

                } catch (Exception e) {
                    invoking.set(false);
                    process = null;
                    outputGobbler = null;
                    if (controlledInterrupt && isExceptionCausedByInterruptRequest(e)) {
                        return;
                    }
                    if (consumeException != null) {
                        try {
                            consumeException.accept(e);
                        } catch (Exception e1) {
                            // just can't win it seems.
                            e1.printStackTrace();
                        }
                    } else {
                        e.printStackTrace();
                    }
                }

                invoking.set(false);
                process = null;
                outputGobbler = null;

            }

        };

        String threadName = "LinuxProcessWatcher-main-" + (this.name != null ? (name + "-") : "") + nextThreadNum();

        controllerThread = new Thread(invoker, threadName);
        controllerThread.start();
    }

    private boolean isExceptionCausedByInterruptRequest(Throwable e) {
        if (e instanceof InterruptedException) {
            return true;
        }

        if (e.getCause() != null) {
            return isExceptionCausedByInterruptRequest(e.getCause());
        }
        return false;
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

    private boolean killViaPIDScriptIfDefined() {

        if (StringUtils.isEmptyStr(this.pidScript)) {
            return false;
        }
        
        try {
            
            int pid = LinuxPIDsUtil.getPidFromScript(this.pidScript);
            
            LinuxPIDsUtil.killAllRelatedProcessesFromPID(pid);
            
            return true;
            
        } catch (Exception e) {
            // assume script is no good - which might happen a lot so let's not be noisy
            Logging.info("Attempted to kill using script cmd '"+this.pidScript+"' .  exception " + e.getMessage());
            return false;
        }
        
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
                    killViaPIDScriptIfDefined();
                    // if the above kill destroys the process
                    // not sure how the following call will behave - time to experiment
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
        try {
            String result = ExecUtil.executeWithShell(this.invokeScript);
            return result;
        } finally {
            invoking.set(false);
        }
    }

    public static String tailCommand(String fileName) throws Exception {

        File fileToTail = new File(fileName);

        if (!fileToTail.exists()) {
            throw new FileNotFoundException(fileName + " not found.  Cannot perform tail.");
        }

        String tailCMD = "/usr/bin/tail -f " + fileToTail.getAbsolutePath();

        return tailCMD;
    }

    public static String pidScriptForTailCommand(String fileName) throws Exception{
        
        File fileToTail = new File(fileName);

        if (!fileToTail.exists()) {
            throw new FileNotFoundException(fileName + " not found.  Cannot perform tail.");
        }

        // the weakness with the following is if there are 2 or more processes tailing the same file
        String pidScript = String.format("ps -ef | grep %s | grep -v -w grep | grep "+ExecUtil.SSH_Path()+" | grep -w tail | awk '{print $2}'",fileToTail.getAbsolutePath());

        return pidScript;
    }

    public static LinuxProcessWatcher tailFile(String name, String fileName) throws Exception {

        String tailCMD = tailCommand(fileName);

        LinuxProcessWatcher watcher = new LinuxProcessWatcher();

        if (name != null) {
            watcher.setName(name);
        }

        watcher.setInvokeScript(tailCMD);
        watcher.setPIDScript(pidScriptForTailCommand(fileName));

        return watcher;

    }

    public static LinuxProcessWatcher tailFile(String fileName) throws Exception {
        return tailFile("tail", fileName);
    }

    public static LinuxProcessWatcher tailFile(String fileName, ConsumeResultAction<String> consumeOutput) throws Exception {

        LinuxProcessWatcher watcher = tailFile(fileName);
        watcher.invokeLongRunning(consumeOutput, (result) -> {
        });

        return watcher;

    }

    public static LinuxProcessWatcher viewTextFile(String fileName) throws Exception {

        File fileToTail = new File(fileName);

        if (!fileToTail.exists()) {
            throw new FileNotFoundException(fileName + " not found.  Cannot perform tail.");
        }

        // TODO: Should not use cat !!!-- should just read the damn file!!!! yeesh..

        String tailCMD = "cat " + fileToTail.getAbsolutePath();

        LinuxProcessWatcher watcher = new LinuxProcessWatcher();

        watcher.setInvokeScript(tailCMD);

        return watcher;

    }

    public static LinuxProcessWatcher viewTextFile(String fileName, ConsumeResultAction<String> consumeOutput)
            throws Exception {

        LinuxProcessWatcher watcher = viewTextFile(fileName);
        watcher.invokeLongRunning(consumeOutput, (result) -> {});

        return watcher;

    }

}