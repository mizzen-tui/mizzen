package dtb.terminalIO.examples.plot.gameoflife;

import java.util.Arrays;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public class GameArrayUtils {
    
    static public void clear2d(int a[][]) {
        Dimension dimA = toDimension(a);
        int height = dimA.getHeight();
        for (int row = 0; row < height; row++) {
            Arrays.fill(a[row], 0);
        }
    }

    static public  int[][] make2d(int height, int width) {
        int a[][] = new int[height][width];
        clear2d(a);
        return a;
    }

    static public int[][] make2d(Dimension d) {
        return make2d(d.getHeight(), d.getWidth());
    }

    static public  Dimension toDimension(int a[][]) {
        if (a.length == 0) {
            return Dimension.ZERO;
        }
        return new Dimension.Simple(a.length, a[0].length);
    }

    static public int[][] copyArraySize(int a[][]) {
        if (a.length == 0) {
            return new int[][] {};
        }
        Dimension dimA = toDimension(a);
        return make2d(dimA.getHeight(), dimA.getWidth());
    }
    
    // copies array 'src' into array dst 
    static public void copyArrayInto(int[][] src, int[][] dst, int offsetY, int offsetX) {
         // copy src to dst but don't go outside the bounds of dst
         Dimension dstDIM = toDimension(dst);
         Dimension srcDIM = toDimension(src);
         
         for (int y = 0; y<srcDIM.getHeight(); y++) {
            int dstY = offsetY + y;
            if (dstY >= dstDIM.getHeight()) {
                break;
            }
            for (int x = 0; x<srcDIM.getWidth(); x++) {
                int dstX = x + offsetX;
                if (dstX>= dstDIM.getWidth()) {
                  break;
                }
                int srcCellValue = src[y][x];
                dst[dstY][dstX] = srcCellValue;
            }
         }

    }

    // copies array 'src' into array dst 
    static public void copyArrayInto(int[][] src, int[][] dst) {
        copyArrayInto(src, dst, 0, 0);
    }

    static public void copyArrayCentered(int[][] src, int[][] dst) {
         Dimension dstDIM = toDimension(dst);
         Dimension srcDIM = toDimension(src);
         Position offsetPoint = new Rectangle.Simple(dstDIM).centered(srcDIM, Position.ZERO);
         copyArrayInto(src, dst, offsetPoint.getRow(), offsetPoint.getCol());
    }

    static public PointPlotter asPointPlotter(int[][] grid) {
        return new PointPlotter() {

            @Override
            public void plot(float x, float y) {
                grid[(int)y][(int)x] = 1;
            }

            @Override
            public Color setNextPlotColor(Color color) {
                return null;
            }

            @Override
            public void clearPlots() {
                clear2d(grid);
            }
        };
    }

    
}
