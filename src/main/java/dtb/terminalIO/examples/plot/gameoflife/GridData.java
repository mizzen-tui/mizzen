package dtb.terminalIO.examples.plot.gameoflife;

import dtb.terminalIO.geom.Dimension;

public class GridData implements Dimension {

    int[][] array2D;
    int generation = 0;
    Dimension size = Dimension.ZERO;

    public void setSize(Dimension size) {
        if (this.array2D == null) {
            this.array2D = GameArrayUtils.make2d(size);
            this.size = new Dimension.Simple(size);
            return;
        }
        int[][] newGrid = GameArrayUtils.make2d(size);
        GameArrayUtils.copyArrayInto(this.array2D, newGrid);
        this.array2D = newGrid;
        this.size = new Dimension.Simple(size);
    }
    

    public void copyShapeIntoGrid(int[][] data) {
        GameArrayUtils.copyArrayInto(data, this.array2D);
    }

    public void copyShapeIntoGridCentered(int[][] data) {
        GameArrayUtils.copyArrayCentered(data, this.array2D);
    }

    public void copyShapeIntoGrid(int[][] data, int offsetY, int offsetX) {
        GameArrayUtils.copyArrayInto(data, this.array2D, offsetY, offsetX);
    }

    @Override
    public int getWidth() {
        if (this.size == null) {
            this.size = GameArrayUtils.toDimension(array2D);
        }
        return this.size.getWidth();
    }

    @Override
    public int getHeight() {
        if (this.size == null) {
            this.size = GameArrayUtils.toDimension(array2D);
        }
        return this.size.getHeight();
    }

    public int[][] getGridData() {
        return this.array2D;
    }

}