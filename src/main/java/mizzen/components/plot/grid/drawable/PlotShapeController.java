package mizzen.components.plot.grid.drawable;

import mizzen.components.plot.PointPlotter;
import mizzen.geom.Rectangle;

public interface PlotShapeController {

    void plot(PointPlotter plotter, Rectangle bounds);

    Rectangle asRectangle();

}
