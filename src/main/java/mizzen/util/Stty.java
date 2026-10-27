package mizzen.util;

import mizzen.geom.Dimension;
import mizzen.util.linuxproc.ExecUtil;

/**
 * Some terminal IO screen utility functions that use the linux stty utility.
 * 
 */
public class Stty {

    static boolean rawSet = false;

    public static Dimension terminalSize() throws Exception {

        String output = ExecUtil.executeWithShell("stty size < /dev/tty");

        // parse output which is the form "cols lines"
        String[] data_split = output.split(" ");
        try {
            int _lines = Integer.parseInt(data_split[0].trim());
            int _cols = Integer.parseInt(data_split[1].trim());

            return new Dimension.Simple(_lines, _cols);
        } catch (NumberFormatException nfe) {
            throw new IllegalStateException(
                    "Error in Stty.terminalSize():  Could not parse lines/columns information when shelling out to stty.  Is this running in a linux environment?");
        }

    }

    private static Boolean stdinIsTerminal;

    /**
     * Is this program's input a terminal? Not when it runs as a child of another program with its
     * input piped - e.g. inside a StdoutViewer. Then raw / cooked mode is left alone: the terminal
     * (/dev/tty) belongs to the parent, and "stty sane cooked" on exit would break the parent's raw mode.
     */
    public static synchronized boolean stdinIsTerminal() {
        if (stdinIsTerminal == null) {
            try {
                Process test = new ProcessBuilder("sh", "-c", "[ -t 0 ]").redirectInput(ProcessBuilder.Redirect.INHERIT).start();
                stdinIsTerminal = test.waitFor() == 0;
            } catch (Exception e) {
                stdinIsTerminal = true;     // can't tell: behave as before
            }
        }
        return stdinIsTerminal;
    }

    public static void setRaw() throws Exception {
        if (!stdinIsTerminal()) {
            return;
        }
        /*
        ExecUtil
                .executeWithShell("stty -ignbrk -brkint -parmrk -istrip " + 
                                  "-inlcr -igncr -icrnl -ixon -opost "+ 
                                  "-echo -echonl -icanon -isig -iexten " + 
                                  "-parenb cs8 min 1" + (OSType.isCYGWIN() ? "" : " < /dev/tty"));
        
        */
        String output = ExecUtil
                .executeWithShell("stty -ignbrk -brkint -parmrk -istrip " + "-inlcr -igncr -icrnl -ixon -opost "
                                     + "-echo -echonl -icanon -isig -iexten " + "-parenb cs8 min 1 < /dev/tty");

        rawSet = true;
    }

    public static void setCooked() throws Exception {
        if (!stdinIsTerminal()) {
            return;
        }
        ExecUtil.executeWithShell("stty sane cooked " + (OSType.isCYGWIN() ? "" : "< /dev/tty"));

        rawSet = false;
    }

    public static boolean isRaw() {
        return rawSet;
    }

    public static boolean isCooked() {
        return !rawSet;
    }

}
