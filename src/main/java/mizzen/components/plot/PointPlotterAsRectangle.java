package mizzen.components.plot;

import mizzen.controlcodes.color.Color;
import mizzen.geom.AsRectangle2D;
import mizzen.geom.Rectangle2D;
import mizzen.geom.Rectangle2D.Float2D;
import mizzen.geom.position.PositionTransform;
import mizzen.geom.position.Vec2;

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
        minMax.setY2(Float.MIN_VALUE);
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
        minMax.setY2(Float.MIN_VALUE);
    }



    @Override
    public Rectangle2D.Float2D getRectangle2D() {
        return (plotStarted) ? minMax : Float2D.ZERO;
    }



    public static class TrackPlottedRectangleTransform implements PositionTransform {

        PointPlotterAsRectangle boundingRectanglePlotter;
        PositionTransform.FofG  f_of_g_transform;

        public TrackPlottedRectangleTransform(PositionTransform baseTransform) {
            PointPlotterAsRectangle boundingRectanglePlotter = new PointPlotterAsRectangle();
            f_of_g_transform = new PositionTransform.FofG(new PositionTransform() {
                @Override
                public Vec2<?> transform(Vec2<?> position) {
                    boundingRectanglePlotter.plot(position);
                    return position;
                }},baseTransform);
        }

        public TrackPlottedRectangleTransform() {
            this(PositionTransform.NOOP);
        }

        @Override
        public Vec2<?> transform(Vec2<?> position) {
            return f_of_g_transform.transform(position);
        }

        public Rectangle2D.Float2D getRectangle2D() {
            return boundingRectanglePlotter.getRectangle2D();
        }

    }

}
