package dtb.terminalIO.util;

import java.io.IOException;

import dtb.terminalIO.geom.Dimension;

/**
 * Some terminal IO screen utility functions that use the linux stty 
 * utlity.
 * 
 */
public class Stty {
    
    static boolean         rawSet      = false;


    public static Dimension terminalSize() throws IOException {
        String output = ExecUtil.executeWithShell("stty size < /dev/tty");
        // String output = executeWithBash("echo `tput cols` `tput lines`");
        // parse output which is the form "cols lines"
        String[] data_split = output.split(" ");
        int _lines = Integer.parseInt(data_split[0].trim());
        int _cols = Integer.parseInt(data_split[1].trim());

        return new Dimension.Simple(_lines, _cols);

    }
    
    public static void setRaw() throws IOException {
        String output = ExecUtil.executeWithShell("stty -ignbrk -brkint -parmrk -istrip " +
                                                       "-inlcr -igncr -icrnl -ixon -opost " + 
                                                       "-echo -echonl -icanon -isig -iexten " +
                                                       "-parenb cs8 min 1 < /dev/tty");
        rawSet = true;
    }

    public static void setCooked() throws IOException {
        //             "/bin/sh", "-c", "stty sane cooked < /dev/tty"
        String output = ExecUtil.executeWithShell("stty sane cooked < /dev/tty");

        rawSet = false;
    }
    
    public static boolean isRaw() {
        return rawSet;
    }

    public static boolean isCooked() {
        return ! rawSet;
    }
    

}
