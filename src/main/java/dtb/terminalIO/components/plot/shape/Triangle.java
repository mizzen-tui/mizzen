package dtb.terminalIO.components.plot.shape;

import java.util.List;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.PointPlotterAsRectangle;
import dtb.terminalIO.components.plot.shape.Shape.AbstractShape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.AsRectangle2D;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.Rectangle2D;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;

/**
 * 
 * Draws a triangle
 * 
 * <pre>
 * 
 *               /-\
 *              /`  \\.
 *            /      '-\
 *          .+          \\.
 *         :/             '-\
 *        /:      ____-------*
 *       :*-------
 * 
 * </pre>
 * 
 * @author dbalme
 *
 */
public class Triangle extends AbstractShape<Triangle> implements Fillable<Triangle> , AsRectangle2D.AsFloat2D  {
    Vec2<?> p1;
    Vec2<?> p2;
    Vec2<?> p3;
    Color color;
    
    DrawTriangleAlgorithm algorithm;

    /**
     * Specifies how to draw the rectangle,
     *    outline, solid
     */
    @FunctionalInterface
    public static interface DrawTriangleAlgorithm {
        void draw(Vec2<?> p1, Vec2<?> p2, Vec2<?> p3, Color color, PointPlotter pointPlotter);
    }

    static DrawTriangleAlgorithm drawOutline = new DrawTriangleAlgorithm() {
        @Override
        public void draw(Vec2<?> p1, Vec2<?> p2, Vec2<?> p3, Color color, PointPlotter pointPlotter) {
            Polygon polygon = new Polygon();
            polygon.addPoint(p1, color);
            polygon.addPoint(p2, color);
            polygon.addPoint(p3, color);
            polygon.draw(pointPlotter);
        }
    };
    
    /** filled: a three-corner polygon, filled (see ConnectedPoints.drawfilled), in the triangle's colour */
    public static DrawTriangleAlgorithm drawFilled = new DrawTriangleAlgorithm() {
        @Override
        public void draw(Vec2<?> p1, Vec2<?> p2, Vec2<?> p3, Color color, PointPlotter pointPlotter) {
            Color lastColor = null;
            if (color != null) {
                lastColor = pointPlotter.setNextPlotColor(color);
            }

            Polygon polygon = new Polygon();
            polygon.addPoint(p1, color);
            polygon.addPoint(p2, color);
            polygon.addPoint(p3, color);
            polygon.setFilled(true);
            polygon.draw(pointPlotter);

            if (color != null) {
                pointPlotter.setNextPlotColor(lastColor);
            }
        }
    };
    
    public Triangle(Position p1, Position p2, Position p3, Color color) {
        super();
        
        List<Vec2<?>> points = Vec2.UTIL.getSorted(p1,p2,p3); 

        this.p1 = points.get(0);
        this.p2 = points.get(1);
        this.p3 = points.get(2);
        this.color = color;
        this.algorithm = drawOutline;
    }
    
    public Rectangle asRectangle() {
        return new Rectangle.Simple((Position)this.p1, Dimension.ZERO)
                   .mergeAndExpandDimensionsWith((Position)this.p2)
                   .mergeAndExpandDimensionsWith((Position)this.p3);
        
    }

    public Triangle(Position p1, Position p2, Position p3) {
        this(p1, p2, p3, null);
    }

    public Triangle(float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
        this(new Position.Simple(y1, x1), new Position.Simple(y2, x2), new Position.Simple(y3, x3), color);
    }

    public Triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
        this(x1, y1, x2, y2, x3, y3, null);
    }

    public Triangle(int x1, int y1, int x2, int y2, int x3, int y3, Color color) {
        this(new Position.Simple(y1, x1), new Position.Simple(y2, x2), new Position.Simple(y3, x3), color);
    }

    public Triangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        this(x1, y1, x2, y2, x3, y3, null);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public Triangle setFilled(boolean filled) {
        this.algorithm = (filled) ? drawFilled : drawOutline;
        return this;
    }
    
    
    @Override
    protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        this.algorithm.draw( positionTransform.transform(p1), 
                             positionTransform.transform(p2), 
                             positionTransform.transform(p3), 
                             color, 
                             pointPlotter);
    }

    /**
     * Utility functions for triangles
     *
     */

    public static class UTIL {

        // following utilties taken from: https://www.geeksforgeeks.org/check-whether-a-given-point-lies-inside-a-triangle-or-not/
        
        /*
         * A utility function to calculate area of triangle formed by (x1, y1) (x2, y2)
         * and (x3, y3)
         */
        static double area(int x1, int y1, int x2, int y2, int x3, int y3) {
            return Math.abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2.0);
        }

        /*
         * A function to check whether point P(x, y) lies inside the triangle formed by
         * A(x1, y1), B(x2, y2) and C(x3, y3)
         */
        static boolean isInside(int x1, int y1, int x2, int y2, int x3, int y3, int x, int y) {
            /* Calculate area of triangle ABC */
            double A = area(x1, y1, x2, y2, x3, y3);

            /* Calculate area of triangle PBC */
            double A1 = area(x, y, x2, y2, x3, y3);

            /* Calculate area of triangle PAC */
            double A2 = area(x1, y1, x, y, x3, y3);

            /* Calculate area of triangle PAB */
            double A3 = area(x1, y1, x2, y2, x, y);

            /* Check if sum of A1, A2 and A3 is same as A */
            return (A == A1 + A2 + A3);
        }
    }

    @Override
    public Rectangle2D getRectangle2D() {
        PointPlotterAsRectangle calculatedRectanglePlotter = new PointPlotterAsRectangle();
        
        calculatedRectanglePlotter.plot(positionTransform.transform(p1));
        calculatedRectanglePlotter.plot(positionTransform.transform(p2));
        calculatedRectanglePlotter.plot(positionTransform.transform(p3));

        return calculatedRectanglePlotter.getRectangle2D();
    }
    

    
}
