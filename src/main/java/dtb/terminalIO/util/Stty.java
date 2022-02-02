package dtb.terminalIO.util;

import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.linuxproc.ExecUtil;

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

    public static void setRaw() throws Exception {
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
