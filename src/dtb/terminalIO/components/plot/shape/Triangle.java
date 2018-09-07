package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.Position;

public class Triangle implements Shape {
    
    Position p1;
    Position p2;
    Position p3;
    
    public Triangle(Position p1, Position p2, Position p3) {
	super();
	this.p1 = p1;
	this.p2 = p2;
	this.p3 = p3;
    }

    public Triangle(int x1, int y1, int x2, int y2, int x3, int y3) {
	this(new Position.Simple(x1,y2), new Position.Simple(x2,y2), new Position.Simple(x3,y3));
    }

    @Override
    public void draw(PointPlotter pointPlotter) {
	Polygon polygon = new Polygon();
	polygon.addPoint(p1);
	polygon.addPoint(p2);
	polygon.addPoint(p3);
	polygon.draw(pointPlotter);
    }
    
    

}
