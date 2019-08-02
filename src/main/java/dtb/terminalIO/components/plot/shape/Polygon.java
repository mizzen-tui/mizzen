package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter.PointPlot;

/**
 * Given a set of points (defined in {@link PointPlot} objects),
 * This draws a shape that is a set of connected lines.
 */
public class Polygon extends ConnectedPoints {
    
    public Polygon() {
        super(true);
    }

}
