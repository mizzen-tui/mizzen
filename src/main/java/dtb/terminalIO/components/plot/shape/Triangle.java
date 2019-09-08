package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.controlcodes.ControlCode.Color;
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

    @Override
    public void draw(PointPlotter pointPlotter) {
        Polygon polygon = new Polygon();
        polygon.addPoint(p1, color);
        polygon.addPoint(p2, color);
        polygon.addPoint(p3, color);
        polygon.draw(pointPlotter);
    }

}
