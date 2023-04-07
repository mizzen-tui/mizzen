package dtb.terminalIO.components.plot.grid.drawable;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.Rectangle;

public interface PlotShapeController {

    void plot(PointPlotter plotter, Rectangle bounds);

    Rectangle asRectangle();

}
