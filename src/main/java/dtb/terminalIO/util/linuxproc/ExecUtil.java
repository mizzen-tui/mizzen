package dtb.terminalIO.util.linuxproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.function.Supplier;

import dtb.terminalIO.util.ConsumeResultAction;
import dtb.terminalIO.util.OSType;

/**
 * Utilities to exec external processes (usually via bash or sh)
 */
public class ExecUtil {

    @FunctionalInterface
    public static interface ConsumeCharOutput {
        void handleChar(char line);
    }

    @FunctionalInterface
    public static interface ConsumeLineOutput extends ConsumeResultAction<String> {
    }

    public static class StringBuilderConsumeOutput implements ConsumeLineOutput {

        StringBuilder sb;

        public StringBuilderConsumeOutput() {
            this.sb = new StringBuilder();
        }

        @Override
        public void accept(String line) {
            sb.append(line);
            sb.append("\n");
        }

        public String toString() {
            return sb.toString();
        }
    }
    
    public static String SSH_Path() {
        return OSType.DETECTED.equals(OSType.CygWin) ? "sh" : "/bin/sh";
    }

    public static String[] shellCMD(String command) {
        return new String[] { SSH_Path(), "-c", command };
    }
    
    public static String executeWithShell(String command) throws Exception {
        return executeCMDAndReturnOutput(new String[] { SSH_Path(), "-c", command });
    }

    public static int executeCMD(String[] commands, ConsumeResultAction<String> outputConsumer) throws Exception {

        Process proc = new ProcessBuilder(commands).start();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        String s = null;
        while ((s = stdInput.readLine()) != null) {
            outputConsumer.accept(s);
        }

        while ((s = stdError.readLine()) != null) {
            outputConsumer.accept(s);
        }
        
        int exitVal = proc.waitFor();
        
        return exitVal;

    }

    public static String executeCMDAndReturnOutput(String[] commands) throws Exception {
        StringBuilderConsumeOutput captureOutput = new StringBuilderConsumeOutput();
        executeCMD(commands, captureOutput);
        return captureOutput.toString();
    }

    public static int executeLongRunning(String[] commands, ConsumeLineOutput outputConsumer,
            Supplier<Boolean> isActive) throws IOException, InterruptedException {

        Runtime rt = Runtime.getRuntime();

        Process proc = rt.exec(commands);
        
        Thread errorGobbler = new Thread(new StreamLineGobbler(proc.getErrorStream(), outputConsumer, isActive),"errorGobbler");

        Thread outputGobbler = new Thread(new StreamLineGobbler(proc.getInputStream(), outputConsumer, isActive),"outputGobbler");

        errorGobbler.start();
        outputGobbler.start();

        int exitVal = proc.waitFor();

        errorGobbler.join();

        outputGobbler.join();  // TODO:  fixme  this does not come back look at LinuxProcessWatcher for a solution (join stdout and stderr)

        return exitVal;
    }

    public static int executeLongRunningWithShell(String command, ConsumeLineOutput outputConsumer,
            Supplier<Boolean> isActive) throws IOException, InterruptedException {
        return executeLongRunning(new String[] { SSH_Path(), "-c", command }, outputConsumer, isActive);
    }

//    public static int executeLongRunningWithShell(String command, 
//                                                  TerminalStreamOutputParser parser,
//                                                  Supplier<Boolean> isActive) throws IOException, InterruptedException {
//        
//        String commands[] = new String[] { "/bin/sh", "-c", command }
//        
//        Runtime rt = Runtime.getRuntime();
//
//        Process proc = rt.exec(commands);
//        
//        Thread errorGobbler = new Thread(new StreamLineGobbler(proc.getErrorStream(), outputConsumer, isActive),"errorGobbler");
//
//        Thread outputGobbler = new Thread(new StreamLineGobbler(proc.getInputStream(), outputConsumer, isActive),"outputGobbler");
//
//        errorGobbler.start();
//        outputGobbler.start();
//
//        int exitVal = proc.waitFor();
//
//        errorGobbler.join();
//
//        outputGobbler.join();  // TODO:  fixme  this does not come back look at LinuxProcessWatcher for a solution (join stdout and stderr)
//
//        return exitVal;
//        
//        
//        return executeLongRunning(new String[] { "/bin/sh", "-c", command }, outputConsumer, isActive);
//    }

    public static class StreamLineGobbler implements Runnable {

        private InputStream is;
//        private Reader      input;
        ConsumeResultAction<String> outputConsumer;
        Supplier<Boolean> isActive;
        boolean isEOF = false;

        public StreamLineGobbler(InputStream is, ConsumeResultAction<String> outputConsumer, Supplier<Boolean> isActive) {
            super();
            this.is = is;
            this.outputConsumer = outputConsumer;
            this.isActive = isActive;
        }

        public StreamLineGobbler(InputStream is, PrintStream os, Supplier<Boolean> isActive) {
            super();
            this.is = is;
            this.outputConsumer = (line) -> os.println(line);
        }

        public StreamLineGobbler(InputStream is, Supplier<Boolean> isActive) {
            this(is, System.out, isActive);
        }

        char buffer[] = new char[1024];

        @Override
        public void run() {
            try {
                String pendingOutput = null;
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                isEOF = false;
                while (isActive.get() && !isEOF && !Thread.interrupted()) {
                    int available = is.available();
                    if (available > 0 && reader.ready()) {
                        int count = reader.read(buffer, 0, Math.min(available / 2, buffer.length));
                        isEOF = (count < 0);
                        if (isEOF) break; // todo - need to ensure that pending gets output at end of loop
                        int j = 0;
                        int i = 0;
                        for (; i < count; i++) {
                            if (buffer[i] == '\n' || buffer[i] == '\r') {
                                String output = ((pendingOutput != null) ? pendingOutput : "") + new String(buffer,j, i-j);
                                j = i+1;
                                pendingOutput = null;
                                outputConsumer.accept(output);
                            }
                        }
                        if (j+1 != i) {
                            pendingOutput = new String(buffer,j, i-j);
                        }
                    } else {
                        Thread.sleep(100);
                    }
                }
                
                if (pendingOutput != null && pendingOutput.length()>0) {
                    outputConsumer.accept(pendingOutput);
                }
                
                reader.close();
                
            } catch (Exception e) {
                // handle error .. how?
                if (isActive.get()) {
                    e.printStackTrace();
                }
            }

        }
    }
    

}
