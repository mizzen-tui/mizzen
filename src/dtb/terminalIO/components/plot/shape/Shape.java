package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;

public interface Shape {
    
    public void draw(PointPlotter pointPlotter);
    
    
    public static class Delegate implements Shape {
	
	Shape shape;
	
	public Delegate(Shape shape) {
	    super();
	    this.shape = shape;
	}

	public Shape getShape() {
	    return shape;
	}

	public void setShape(Shape shape) {
	    this.shape = shape;
	}

	@Override
	public void draw(PointPlotter pointPlotter) {
	    this.getShape().draw(pointPlotter);
	}
	
    }
    

}
