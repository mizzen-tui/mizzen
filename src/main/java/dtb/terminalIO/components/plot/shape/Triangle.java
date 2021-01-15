package dtb.terminalIO.components.plot.shape;

import java.awt.geom.Path2D;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.position.Position;

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
public class Triangle implements Shape {

    Position p1;
    Position p2;
    Position p3;
    Color color;

    public Triangle(Position p1, Position p2, Position p3, Color color) {
        super();
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.color = color;
    }

    public Triangle(Position p1, Position p2, Position p3) {
        this(p1, p2, p3, null);
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
    public void draw(PointPlotter pointPlotter) {
        
        Path2D myPath = new Path2D.Double();
        
        Polygon polygon = new Polygon();
        polygon.addPoint(p1, color);
        polygon.addPoint(p2, color);
        polygon.addPoint(p3, color);
        polygon.draw(pointPlotter);
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

}
