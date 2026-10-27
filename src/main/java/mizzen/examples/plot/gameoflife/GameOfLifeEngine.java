package mizzen.examples.plot.gameoflife;

import mizzen.geom.Dimension;

public class GameOfLifeEngine  {


    private int sumNeighbours2D(int a[][], int y, int x) {
        int sum = 0;
        Dimension dimA = GameArrayUtils.toDimension(a);
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
        int[][] neighbours2D = GameArrayUtils.copyArraySize(a);
        Dimension dimA = GameArrayUtils.toDimension(a);
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
        int[][] nextGen2D = GameArrayUtils.copyArraySize(a);
        Dimension dimA = GameArrayUtils.toDimension(a);
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


}

