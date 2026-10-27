package mizzen.components.plot.grid;

import mizzen.components.Component;
import mizzen.components.plot.Plotter;
import mizzen.components.plot.PointPlotter;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

public class PlotterGridContext implements GridContext {
    Plotter grid;

    public PlotterGridContext(Plotter grid) {
        super();
        this.grid = grid;
    }

    @Override
    public PointPlotter asPointPlotter() {
        return this.grid.asPointPlotter();
    }

    @Override
    public Rectangle getGridRectangle() {
        return this.grid.getPreferredRectangleForLayout();
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
        return charPosition;
    }

}
