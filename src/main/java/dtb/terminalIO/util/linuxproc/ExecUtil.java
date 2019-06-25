package dtb.terminalIO.util.linuxproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * Utilities to exec external processes (usually via bash or sh)
 */
public class ExecUtil {

    @FunctionalInterface
    public static interface ConsumeCharOutput {
        void handleChar(char line);
    }

    @FunctionalInterface
    public static interface ConsumeLineOutput {
        void handleLine(String line) throws IOException;
    }

    public static class StringBuilderConsumeOutput implements ConsumeLineOutput {

        StringBuilder sb;

        public StringBuilderConsumeOutput() {
            this.sb = new StringBuilder();
        }

        @Override
        public void handleLine(String line) {
            sb.append(line);
            sb.append("\n");
        }

        public String toString() {
            return sb.toString();
        }
    }

    public static String executeWithShell(String command) throws IOException {
        return executeCMDAndReturnOutput(new String[] { "/bin/sh", "-c", command });
    }

    public static void executeCMD(String[] commands, ConsumeLineOutput outputConsumer) throws IOException {

        Process proc = new ProcessBuilder(commands).start();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        String s = null;
        while ((s = stdInput.readLine()) != null) {
            outputConsumer.handleLine(s);
        }

        while ((s = stdError.readLine()) != null) {
            outputConsumer.handleLine(s);
        }

    }

    public static String executeCMDAndReturnOutput(String[] commands) throws IOException {
        StringBuilderConsumeOutput captureOutput = new StringBuilderConsumeOutput();
        executeCMD(commands, captureOutput);
        return captureOutput.toString();
    }

    public static int executeLongRunning(String[] commands, ConsumeLineOutput outputConsumer,
            Supplier<Boolean> isActive) throws IOException, InterruptedException {

        Runtime rt = Runtime.getRuntime();

        Process proc = rt.exec(commands);
        
        Thread errorGobbler = new Thread(new StreamLineGlobber(proc.getErrorStream(), outputConsumer, isActive),"errorGobbler");

        Thread outputGobbler = new Thread(new StreamLineGlobber(proc.getInputStream(), outputConsumer, isActive),"outputGobbler");

        errorGobbler.start();
        outputGobbler.start();

        int exitVal = proc.waitFor();

        errorGobbler.join();

        outputGobbler.join();

        return exitVal;
    }

    public static int executeLongRunningWithShell(String command, ConsumeLineOutput outputConsumer,
            Supplier<Boolean> isActive) throws IOException, InterruptedException {
        return executeLongRunning(new String[] { "/bin/sh", "-c", command }, outputConsumer, isActive);
    }

    public static class StreamLineGlobber implements Runnable {

        private InputStream is;
        ConsumeLineOutput outputConsumer;
        Supplier<Boolean> isActive;

        public StreamLineGlobber(InputStream is, ConsumeLineOutput outputConsumer, Supplier<Boolean> isActive) {
            super();
            this.is = is;
            this.outputConsumer = outputConsumer;
            this.isActive = isActive;
        }

        public StreamLineGlobber(InputStream is, PrintStream os, Supplier<Boolean> isActive) {
            super();
            this.is = is;
            this.outputConsumer = (line) -> os.println(line);
        }

        public StreamLineGlobber(InputStream is, Supplier<Boolean> isActive) {
            this(is, System.out, isActive);
        }

        @Override
        public void run() {
            try {
                String s;
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                while (isActive.get() && (s = reader.readLine()) != null) {
                    outputConsumer.handleLine(s);
                }
            } catch (IOException e) {
                // handle error .. how?
            }

        }
    }

}
