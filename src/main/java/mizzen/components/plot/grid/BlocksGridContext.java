package mizzen.components.plot.grid;

import mizzen.components.Component;
import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.blocks.BlocksGrid;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

public class BlocksGridContext implements GridContext{
    BlocksGrid grid;

    public BlocksGridContext(BlocksGrid grid) {
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
    public Component<?> asComponent() {
        return this.grid;
    }

    @Override
    public Position mapCharPositionToGridPosition(Position charPosition) {
        int row = charPosition.getRow() * 2;
        int col = charPosition.getCol();
        return new Position.Simple(row, col);
    }

}
