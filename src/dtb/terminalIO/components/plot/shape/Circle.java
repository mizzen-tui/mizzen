package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;

public class Circle implements Shape {
    
    int x; 
    int y;
    int radius;
    
    public Circle(){}
    
    public Circle(int x, int y, int radius) {
	super();
	this.x = x;
	this.y = y;
	this.radius = radius;
    }

    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
    public int getRadius() {
        return radius;
    }
    public void setRadius(int radius) {
        this.radius = radius;
    }
    
    public void draw(PointPlotter pointPlotter) {
	new CirclePlotter().draw(this.getX(), this.getY(), this.getRadius(), pointPlotter);
    }
    

}
