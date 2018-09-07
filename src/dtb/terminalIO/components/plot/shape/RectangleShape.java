package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.Rectangle;

public class RectangleShape implements Shape {
    
    Rectangle rectangle;
    
    public RectangleShape(Rectangle rect) {
	this.rectangle = rect;
    }

    @Override
    public void draw(PointPlotter pointPlotter) {
	Polygon polygon = new Polygon();
	polygon.addPoint( this.rectangle.getPostion_TopLeft() );
	polygon.addPoint( this.rectangle.getPostion_TopRight() );
	polygon.addPoint( this.rectangle.getPostion_BottomRight() );
	polygon.addPoint( this.rectangle.getPostion_BottomLeft() );
	
	polygon.draw(pointPlotter);
    }
    

}
