package dtb.terminalIO.util;

import java.io.IOException;

import dtb.terminalIO.geom.Dimension;

public class TerminalDimensions implements Dimension {

    private int height;
    private int width;

    protected TerminalDimensions(int height, int width) {
        this.height = height;
        this.width = width;
    }
    
    public TerminalDimensions(Dimension ttySize) {
        this(ttySize.getHeight(), ttySize.getWidth());
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public static TerminalDimensions create()
    {
        String osname = System.getProperty("os.name");

        // try to get terminal dimensions from env vars setup by clish action
        // script
        String sCols = getEnv("TERMINAL_WIDTH", null);
        if (sCols != null)
        {
            String sLines = getEnv("TERMINAL_HEIGHT", null);
            if (sLines != null)
            {
                int _cols = Integer.parseInt(sCols);
                int _lines = Integer.parseInt(sLines);
                return new TerminalDimensions(_lines, _cols);
            }
        }

        // try to get dimensions by shelling out to os and asking

        // set to default - useful when we cannot extract from o/s
        int cols = 105;
        int lines = 24;

        if (osname.contains("inux") || osname.contains("nix") || osname.contains("mac"))
        { // linux/unix/mac
            try
            {
                Dimension ttySize = Stty.terminalSize();
                return new TerminalDimensions(ttySize);

            } catch (IOException e)
            {
                // do nothing and use defaults - maybe log error?
            }
        } // can't find a reliable locale safe way to get windows terminal
          // dimensions - so use defaults

        return new TerminalDimensions(lines, cols);
    }

    @Override
    public String toString()
    {
        return "TerminalDimensions [columns=" + width + ", lines=" + height + "]";
    }


    private static String getEnv(String name, String defaultValue) {
        String value =  System.getenv(name);
        if (value != null) return value;
        return defaultValue;
    }

    public static void main(String[] args)
    {
        System.out.println(TerminalDimensions.create());
    }
    
    
}
