package dtb.terminalIO.components.plot;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.AsRectangle2D;
import dtb.terminalIO.geom.Rectangle2D;
import dtb.terminalIO.geom.Rectangle2D.Float2D;

/**
 * A point plotter that only records plots in order to capture the 
 * min x, min y, max x and max y in order to calculate a "bounding-box" rectangle 
 * of all the plots.
 * 
 * <p/>
 * 
 * Useful when trying to capture bounding box of a shape.  Simply draw (expensive!) the shape 
 * into this point plotter and the bounding box will be determined.
 * 
 * 
 * @author dbalme
 *
 */
public class PointPlotterAsRectangle implements PointPlotter, AsRectangle2D.AsFloat2D {

    Rectangle2D.Float2D minMax;
    boolean plotStarted = false;
    
    public PointPlotterAsRectangle() {
        minMax = new Rectangle2D.Float2D();
        minMax.setX1(Float.MAX_VALUE);
        minMax.setY1(Float.MAX_VALUE);
        minMax.setX2(Float.MIN_VALUE);
        minMax.setY1(Float.MIN_VALUE);
    }

    public PointPlotterAsRectangle(Float2D initialSize) {
        this.minMax = initialSize;
    }

    @Override
    public void plot(float x, float y) {
        plotStarted = true;
        minMax.setX1( Math.min(x, minMax.getX1()) );
        minMax.setY1( Math.min(y, minMax.getY1()) );
        minMax.setX2( Math.max(x, minMax.getX2()) );
        minMax.setY2( Math.max(y, minMax.getY2()) );
    }

    @Override
    public Color setNextPlotColor(Color color) {
        // ignore
        return null;
    }

    @Override
    public void clearPlots() {
        plotStarted = false;
        minMax = new Rectangle2D.Float2D();
        minMax.setX1(Float.MAX_VALUE);
        minMax.setY1(Float.MAX_VALUE);
        minMax.setX2(Float.MIN_VALUE);
        minMax.setY1(Float.MIN_VALUE);
    }



    @Override
    public Rectangle2D.Float2D getRectangle2D() {
        return (plotStarted) ? minMax : Float2D.ZERO;
    }




}
