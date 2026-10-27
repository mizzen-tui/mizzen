package mizzen.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Logging {

    // FIXME : would be better if an real logger was used..  Issue is..
    //         don't want this lib to depend on any external libs.  But I could (should)
    //         make it possible to have this point to a real logger.
    //         Maybe make a logging "bridge" class that allows for this connection to
    //         something like log4j
    
    
    static boolean enabled = false;
    static String loggerFileName;
    static Object mutex = new Object();
    
    public static void setEnabled(boolean enabled) {
        Logging.enabled = enabled;
    }

    public static void setEnabledFromArgs(String args[]) {
        VeryBasicArgsHelper.onNamedBooleanArg(args,"LoggingEnabled",(value)-> setEnabled(value));
    }
    
    public static boolean isEnabled() {
        return Logging.enabled ;
    }
    
    private static String getLoggerFileName() throws FileNotFoundException, IOException  {
        if (loggerFileName == null) {
            String pwd = System.getProperty("user.dir");
            Path path = Paths.get(pwd, "dtbTerminal.log");
            loggerFileName = path.toString();
            if ( ! new File(loggerFileName).exists()) {
                FileOutputStream stream = new FileOutputStream(new File(loggerFileName));
                stream.write("dtbTerminal UI logger\n\n".getBytes());
                stream.close();
            }
        }
        return loggerFileName;
    }
    
    public static void info(String msg) {
        if (enabled) {
            appendText(msg);
        } 
    }
    
    public static void forceLogInfo(String msg) {
        boolean enabled = isEnabled();
        if (!enabled) {
             setEnabled(true);
        }
        info(msg);
        Logging.setEnabled(enabled);
    }
    
    private static void appendText(String msg) {
        try {
            // todo:  FIX THIS!!
            Thread.currentThread().getName();
            msg = Thread.currentThread().getName() + ":  " + msg;
            // todo: add date
            synchronized (mutex) {
                Files.write(Paths.get(getLoggerFileName()), (msg + "\n").getBytes(), StandardOpenOption.APPEND);
            }
        }catch (IOException e) {
            // exception handling left as an exercise for the reader
            System.out.println(e.toString());
        }        
    }
    
    
    public static void main(String[] args) {

        Logging.setEnabled(true);
        
        Logging.info("here is a test");

    }
    
    public static class LogOnInterval {
        ThrottledRunnable throttled;

        public boolean isEnabled() {
            return Logging.isEnabled();
        }

        public LogOnInterval(long interval) {
            super();
            this.throttled = new ThrottledRunnable(interval);
        }

        public LogOnInterval() {
            this(5000);
        }

        public void info(String msg) {
            throttled.run(()-> Logging.info(msg));
        }

    }

}
