package dtb.terminalIO.examples.plot.gameoflife;

import java.util.StringJoiner;

import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;

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
    
    private String cellAsString(int cellValue) {
       if (cellValue == 0) {
          return " ";
       }
       return Colors.coloredText(Colors.IYel,"▖");
       //return '▖';
       //return '#';
       
       //return cellValue = ""+cellValue;
    }

    
    private String array1DAsString(int[] a) {
        StringJoiner  columnJoiner = new StringJoiner(GRID_CELL_DIVIDER+"");
        for (int i=0; i<a.length;i++) {
            columnJoiner.add(cellAsString(a[i])+"");
        }
        return columnJoiner.toString();
    }

    public Dimension calcGameBoardDimension(Dimension dataDimension) {
        return new Dimension.Simple(dataDimension.getHeight(), dataDimension.getWidth()*2 - 1) ;
    }

    public String array2DAsString(int[][] a) {
        StringJoiner  rowJoiner = new StringJoiner("\n");
        for (int i=0; i<a.length; i++) {
            rowJoiner.add(array1DAsString(a[i]));
        }
        return rowJoiner.toString();
    }


}
