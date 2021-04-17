package dtb.terminalIO.examples.plot.gameoflife;

import java.util.Arrays;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;

public class GameOfLifeEngine  {


    public void clearGrid(int a[][]) {
        Dimension dimA = toDimension(a);
        int height = dimA.getHeight();
        for (int row = 0; row < height; row++) {
            Arrays.fill(a[row], 0);
        }
    }

    public int[][] make2d(int height, int width) {
        int a[][] = new int[height][width];
        clearGrid(a);
        return a;
    }

    public int[][] make2d(Dimension d) {
        return make2d(d.getHeight(), d.getWidth());
    }

    public Dimension toDimension(int a[][]) {
        if (a.length == 0) {
            return Dimension.ZERO;
        }
        return new Dimension.Simple(a.length, a[0].length);
    }

    private int[][] copyArraySize(int a[][]) {
        if (a.length == 0) {
            return new int[][] {};
        }
        Dimension dimA = toDimension(a);
        return make2d(dimA.getHeight(), dimA.getWidth());
    }

    private int sumNeighbours2D(int a[][], int y, int x) {
        int sum = 0;
        Dimension dimA = toDimension(a);
        int height = dimA.getHeight();
        int width = dimA.getWidth();
        // (y-1,x-1)
        if (y - 1 >= 0 && x - 1 >= 0) {
            sum = sum + a[y - 1][x - 1];
        }

        // (y-1,x)
        if (y - 1 >= 0 && x >= 0) {
            sum = sum + a[y - 1][x];
        }
        // (y-1,x+1)
        if (y - 1 >= 0 && x + 1 < width) {
            sum = sum + a[y - 1][x + 1];
        }

        // (y,//x+1)
        if (y - 1 >= 0 && x - 1 >= 0) {
            sum = sum + a[y][x - 1];
        }

        // (y,x+1)
        if (y >= 0 && x + 1 < width) {
            sum = sum + a[y][x + 1];
        }

        // (y+1,x-1)
        if (y + 1 < height && x - 1 >= 0) {
            sum = sum + a[y + 1][x - 1];
        }

        // (y+1,x)
        if (y + 1 < height && x >= 0) {
            sum = sum + a[y + 1][x];
        }

        // (y+1,x+1)
        if (y + 1 < height && x + 1 < width) {
            sum = sum + a[y + 1][x + 1];
        }

        return sum;

    }

    private int[][] makeNeighboursArray2D(int a[][]) {
        int[][] neighbours2D = copyArraySize(a);
        Dimension dimA = toDimension(a);
        int height = dimA.getHeight();
        int width = dimA.getWidth();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sum = sumNeighbours2D(a, y, x);
                neighbours2D[y][x] = sum;
            }
        }
        return neighbours2D;

    }
    
    private boolean doILive(int neighboursCount, int cellState) {
        //1 or less dies
        if (neighboursCount <= 1 && cellState == 1) {
          return false;
        }

        //4 or more dies
        if (neighboursCount >= 4 && cellState == 1) {
          return false;
        }

        //2 or 3 lives
        if ((neighboursCount ==2 || neighboursCount ==3) && cellState == 1) {
          return true;
        }

        //3 neighbours becomes populated
        if (neighboursCount ==3  && cellState == 0) {
          return true;
        }

        return false;
    }

    public int[][] makeNextGeneration(int a[][]) {
        int[][] nextGen2D = copyArraySize(a);
        Dimension dimA = toDimension(a);
        int height = dimA.getHeight();
        int width = dimA.getWidth();
        int[][] neighboursCount2D = makeNeighboursArray2D(a);
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++) {
            nextGen2D [y][x] = doILive(neighboursCount2D [y][x], a[y][x]) ? 1 : 0;
          }
        }
        return nextGen2D;
    }

    PointPlotter asPointPlotter(int[][] grid) {
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
                clearGrid(grid);
            }
            
        };
    }

}

