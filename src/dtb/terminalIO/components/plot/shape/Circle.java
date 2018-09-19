package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.components.plot.PointPlotter;

public class Circle implements Shape {
    
    int x; 
    int y;
    int radius;
    Color color;
    
    public Circle(){}
    
    public Circle(int x, int y, int radius) {
	super();
	this.x = x;
	this.y = y;
	this.radius = radius;
    }

    public Circle(int x, int y, int radius, Color color) {
	this.x = x;
	this.y = y;
	this.radius = radius;
	this.color = color;
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
	Color lastColor = null;
	if (color != null) {
	    lastColor = pointPlotter.setColor(color);
	}
	
	new CirclePlotter().draw(this.getX(), this.getY(), this.getRadius(), pointPlotter);
	
	if (color != null) {
	    pointPlotter.setColor(lastColor);
	}
    }
    
    
    
}
