package dtb.terminalIO.components.plot.grid;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public class BrailleGridContext implements GridContext {
    BrailleGrid grid;

    public BrailleGridContext(BrailleGrid grid) {
        super();
        this.grid = grid;
    }

    @Override
    public PointPlotter asPointPlotter() {
        return this.grid.asPointPlotter();
    }

    @Override
    public Rectangle getGridRectangle() {
        return this.grid.getGridRectangle();
    }

    @Override
    public boolean isDirty() {
        return this.grid.isDirty();
    }

    @Override
    public Component asComponent() {
        return this.grid;
    }

    @Override
    public Position mapCharPositionToGridPosition(Position charPosition) {
        int row = charPosition.getRow() * 4 + 2;
        int col = charPosition.getCol() * 2;
        return new Position.Simple(row, col);
    }
}
