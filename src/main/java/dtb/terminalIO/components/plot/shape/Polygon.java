package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter.PointPlot;

/**
 * Given a set of points (defined in {@link PointPlot} objects),
 * This draws a shape that is a set of connected lines.
 */
public class Polygon extends ConnectedPoints<Polygon> {
    
    public Polygon() {
        super(true);
    }

    public Polygon(int[][] dots) {
        super(true);
        addPoints(dots);
    }
    
    public void addPoints(int[][] dots) {
        for (int[] p : dots) {
            float x = p[0];
            float y = p[1];
            addPoint( x,y );
        }
    }
    
    
    

}
