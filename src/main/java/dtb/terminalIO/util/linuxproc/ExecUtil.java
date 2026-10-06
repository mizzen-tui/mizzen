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
    
    public static String SH_Path() {
        return OSType.DETECTED.equals(OSType.CygWin) ? "sh" : "/bin/sh";
    }

    public static String[] shellCMD(String command) {
        return new String[] { SH_Path(), "-c", command };
    }
    
    public static String executeWithShell(String command) throws Exception {
        return executeCMDAndReturnOutput(new String[] { SH_Path(), "-c", command });
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
        return executeLongRunning(new String[] { SH_Path(), "-c", command }, outputConsumer, isActive);
    }
    


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

        // the start of a line whose end has not arrived yet
        private String pendingOutput = null;

        /**
         * Hands on each line of the program's output as it completes (a line ends at \n or \r); a
         * partial line waits for the rest. The read blocks until the program writes something or its
         * output ends - no polling - and the thread ends at the end of the output (or when isActive
         * says to stop, at the next read).
         */
        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                isEOF = false;
                while (isActive.get() && !isEOF && !Thread.interrupted()) {
                    isEOF = !readAndHandOn(reader);
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

        /** one read: complete lines handed on, the rest kept pending. false at end of stream. */
        private boolean readAndHandOn(BufferedReader reader) throws Exception {
            int count = reader.read(buffer, 0, buffer.length);
            if (count < 0) {
                return false;
            }
            int j = 0;
            for (int i = 0; i < count; i++) {
                if (buffer[i] == '\n' || buffer[i] == '\r') {
                    String output = ((pendingOutput != null) ? pendingOutput : "") + new String(buffer, j, i - j);
                    j = i + 1;
                    pendingOutput = null;
                    outputConsumer.accept(output);
                }
            }
            // what follows the last line break starts the next line - all of it (even one char: often
            // an ESC), added to what is pending (a read with no line break at all)
            if (j < count) {
                String rest = new String(buffer, j, count - j);
                pendingOutput = (pendingOutput != null) ? pendingOutput + rest : rest;
            }
            return true;
        }
    }
    

}
