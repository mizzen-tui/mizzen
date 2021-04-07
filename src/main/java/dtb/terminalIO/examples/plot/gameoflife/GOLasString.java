package dtb.terminalIO.examples.plot.gameoflife;

public class GOLasString {
    

    public static char GRID_CELL_DIVIDER         = ' ';
    public static char GRID_TOP_LEFT_CORNER      = '+';
    public static char GRID_TOP_RIGHT_CORNER     = '+';
    public static char GRID_BOTTOM_LEFT_CORNER   = '+';
    public static char GRID_BOTTOM_RIGHT_CORNER  = '+';
    public static char GRID_BORDER_VERTICAL      = ' ';
    public static char GRID_BORDER_HORIZONTAL    = '-';

    private char cellAsStringXXXX(int cellValue) {
        return 'x';
    }
    private char cellAsString(int cellValue) {
       if (cellValue == 0) {
          return ' ';
       }
       //return cellValue = termIO.coloredStr(termIO.IYel,"▖");
       //return '▖';
       return '#';
       
       //return cellValue = ""+cellValue;
    }

    private String array1DAsString(int[] a) {
        String output = cellAsString(a[0])+"";
        for (int i=1; i<a.length;i++) {
            output += GRID_CELL_DIVIDER+""+cellAsString(a[i]);
        }
        return output;
    }


    public String array2DAsString(int[][] a) {
        String output = GRID_BORDER_VERTICAL+" "+array1DAsString(a[0])+" "+GRID_BORDER_VERTICAL;
        int maxlen = -1;
        for (int i=1; i<a.length;i++) {
            String lineOutput = array1DAsString(a[i]);
            if (maxlen<lineOutput.length()) {
              maxlen = lineOutput.length();
            }
            lineOutput = GRID_BORDER_VERTICAL+" "+array1DAsString(a[i])+" "+GRID_BORDER_VERTICAL;
            output += "\n"+lineOutput;
        }
        return  output;
    }


}
