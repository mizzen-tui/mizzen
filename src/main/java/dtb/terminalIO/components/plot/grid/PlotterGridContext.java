package dtb.terminalIO.components.plot.grid;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.plot.Plotter;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

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
