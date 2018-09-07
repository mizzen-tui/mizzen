package dtb.terminalIO.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utilities to exec external processes (usually via bash or sh)
 */
public class ExecUtil {

    
    public static String executeWithShell(String command) throws IOException
    {
        return executeCMDAndReturnOutput(new String[] { "/bin/sh", "-c", command });
    }
    
    
    public static String executeCMDAndReturnOutput(String[] commands) throws IOException  {
        StringBuilder sb = new StringBuilder();

        Process proc = new ProcessBuilder(commands).start();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        String s = null;
        while ((s = stdInput.readLine()) != null)
        {
            sb.append(s);
            sb.append("\n");
        }

        while ((s = stdError.readLine()) != null)
        {
            sb.append(s);
            sb.append("\n");
        }
        return sb.toString();

    }
    
    
    
}
