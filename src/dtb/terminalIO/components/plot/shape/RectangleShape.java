package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.Rectangle;

public class RectangleShape implements Shape {
    
    Rectangle rectangle;
    Color color;

    public RectangleShape(Rectangle rect, Color color) {
        this.rectangle = rect;
        this.color = color;
    }

    public RectangleShape(Rectangle rect) {
	this(rect, null);
    }

    @Override
    public void draw(PointPlotter pointPlotter) {
	Polygon polygon = new Polygon();
	polygon.addPoint( this.rectangle.getPostion_TopLeft() , color);
	polygon.addPoint( this.rectangle.getPostion_TopRight() , color);
	polygon.addPoint( this.rectangle.getPostion_BottomRight() , color);
	polygon.addPoint( this.rectangle.getPostion_BottomLeft() , color);
	
	polygon.draw(pointPlotter);
    }
    

}
