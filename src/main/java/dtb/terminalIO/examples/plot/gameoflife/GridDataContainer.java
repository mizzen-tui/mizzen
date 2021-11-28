package dtb.terminalIO.examples.plot.gameoflife;

import dtb.terminalIO.geom.Dimension;

public class GridDataContainer implements Dimension {
    int[][] grid;
    int generation = 0;
    Dimension size = Dimension.ZERO;

    public void setSize(Dimension size) {
        if (this.grid == null) {
            this.grid = GameArrayUtils.make2d(size);
            return;
        }
        int[][] newGrid = GameArrayUtils.make2d(size);
        GameArrayUtils.copyArrayInto(this.grid, newGrid);
        this.grid = newGrid;
        this.size = new Dimension.Simple(size);
    }
    

    public void copyShapeIntoGrid(int[][] data) {
        GameArrayUtils.copyArrayInto(data, this.grid);
    }

    public void copyShapeIntoGridCentered(int[][] data) {
        GameArrayUtils.copyArrayCentered(data, this.grid);
    }

    public void copyShapeIntoGrid(int[][] data, int offsetY, int offsetX) {
        GameArrayUtils.copyArrayInto(data, this.grid, offsetY, offsetX);
    }

    @Override
    public int getWidth() {
        if (this.size == null) {
            this.size = GameArrayUtils.toDimension(grid);
        }
        return this.size.getWidth();
    }

    @Override
    public int getHeight() {
        if (this.size == null) {
            this.size = GameArrayUtils.toDimension(grid);
        }
        return this.size.getHeight();
    }

    public int[][] getGridData() {
        return this.grid;
    }

}