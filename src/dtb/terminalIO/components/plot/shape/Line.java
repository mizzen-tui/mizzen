package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.Position;

public class Line implements Shape {

    int x1;
    int y1;
    int x2;
    int y2;
    
    public Line() {}
    
    public Line(int x1, int y1, int x2, int y2) {
	super();
	this.x1 = x1;
	this.y1 = y1;
	this.x2 = x2;
	this.y2 = y2;
    }

    public Line(Position p1, Position p2) {
	this(p1.getCol(), p1.getRow(), p2.getCol(), p2.getRow());
    }

    public int getX1() {
        return x1;
    }
    public void setX1(int x1) {
        this.x1 = x1;
    }
    public int getY1() {
        return y1;
    }
    public void setY1(int y1) {
        this.y1 = y1;
    }
    public int getX2() {
        return x2;
    }
    public void setX2(int x2) {
        this.x2 = x2;
    }
    public int getY2() {
        return y2;
    }
    public void setY2(int y2) {
        this.y2 = y2;
    }

    @Override
    public void draw(PointPlotter pointPlotter) {
	new LinePlotter().draw(this.getX1(), this.getY1(), this.getX2(), this.getY2(), pointPlotter);
    }
    
}
