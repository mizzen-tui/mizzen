package mizzen.util;

import java.util.Locale;
import java.util.Properties;

import mizzen.util.linuxproc.ExecUtil;

public enum OSType {
    
    Windows, MacOS, Linux, CygWin, Other;
    
    public static final  OSType DETECTED;
    static{
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((OS.contains("mac")) || (OS.contains("darwin"))) {
            DETECTED = OSType.MacOS;
        } else if (OS.contains("win")) {
            if (checkIsCYGWin()) {
                DETECTED = CygWin;
            } else {
                DETECTED = OSType.Windows;
            }
        } else if (OS.contains("nux")) {
            DETECTED = OSType.Linux;
        } else {
            DETECTED = OSType.Other;
        }
    }
    
    public static boolean isCYGWIN() {
        return CygWin.equals(DETECTED);
    }
    
    private static boolean checkIsCYGWin() {
        if (!System.getProperty("java.io.tmpdir", "NOTDEFINED").contains("cygwin")) {
            return false;
        }
        try {
            String unameOutput = ExecUtil.executeCMDAndReturnOutput(new String[] {"uname"});
            if (unameOutput.contains("CYGWIN")) {
                return true;
            }
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }
    

    public static void main(String args[]) throws Exception {
        System.out.println("OS = " + System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)+"\n------------------------------\n\n");
        
        Properties props = System.getProperties();
        
        for (Object p : props.keySet()) {
            System.out.println(String.valueOf(p)+"  =  " + System.getProperty(String.valueOf(p)));
        }

        System.out.println("\n\n\n OS TYPE DETECTED = "+OSType.DETECTED);
    }
}
