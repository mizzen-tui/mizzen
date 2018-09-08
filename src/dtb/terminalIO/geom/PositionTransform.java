package dtb.terminalIO.geom;

/**
 * Transforms Position objects from one location to another.
 * 
 * <p/>
 * 
 * Useful when moving things about the screen
 * 
 */
public interface PositionTransform {

    Position transform(Position position);


    /** 
     * Perform transforms in a FofG like manner..
     * 
     * Useful when you want to make complex transforms from simple transforms.
     *
     */
    public static class FofG implements PositionTransform {
	
	PositionTransform f_transform;
	PositionTransform g_transform;
	
	public FofG(PositionTransform f_transform, PositionTransform g_transform) {
	    super();
	    this.f_transform = f_transform;
	    this.g_transform = g_transform;
	}

	@Override
	public Position transform(Position position) {
	    return f_transform.transform(g_transform.transform(position));
	}
	
    }
    
    public static class Translate implements PositionTransform {
	int rowDelta;
	int colDelta;

	public Translate(int rowDelta, int colDelta) {
	    super();
	    this.rowDelta = rowDelta;
	    this.colDelta = colDelta;
	}

	public Translate(Position delta) {
	    this(delta.getRow(), delta.getCol());
	}

	public int getRowDelta() {
	    return rowDelta;
	}

	public void setRowDelta(int rowDelta) {
	    this.rowDelta = rowDelta;
	}

	public int getColDelta() {
	    return colDelta;
	}

	public void setColDelta(int colDelta) {
	    this.colDelta = colDelta;
	}

	@Override
	public Position transform(Position position) {
	    return new Position.Simple(position.getRow()+getRowDelta(), 
		                       position.getCol()+getColDelta());
	}

    }
    
    /**
     * Rotates a position around the origin
     */
    public static class Rotate implements PositionTransform {

	double theta ; // in radians

	public Rotate(int thetaInDegrees) {
	    super();
	    this.theta =  Math.toRadians(thetaInDegrees);
	}

	public int getTheta() {
	    return (int) Math.toDegrees(theta);
	}

	public void setTheta(int thetaInDegrees) {
	    this.theta = Math.toRadians(thetaInDegrees);;
	}

	@Override
	public Position transform(Position position) {
	    int rotatedX = (int) (position.getCol()*Math.cos(theta) -
		                  position.getRow()*Math.sin(theta));
	    int rotatedY = (int) (position.getRow()*Math.cos(theta) +
	                          position.getCol()*Math.sin(theta));
	    return new Position.Simple(rotatedY, rotatedX);
	}
    }

    /**
     * Rotates a position around a fixed point
     */
    public static class RotateAroundFixedPoint implements PositionTransform {

	int theta;
	Position fixedPoint;
	
	PositionTransform positionTransform;

	public RotateAroundFixedPoint(int theta, Position fixedPoint) {
	    super();
	    this.theta = theta;
	    this.fixedPoint = fixedPoint;
	    
	    makeTransform();
	}
	
	private void makeTransform() {
	    
	    // This transform will translate position relative to origin,
	    // then perform the rotate and then translate back to fixed point
	    positionTransform = new FofG(new Translate(fixedPoint),
		                 new FofG(new Rotate(theta),new Translate(fixedPoint.negate()) ));
	}
	

	public int getTheta() {
	    return theta;
	}

	public void setTheta(int theta) {
	    this.theta = theta;
	    makeTransform();
	}

	public Position getFixedPoint() {
	    return fixedPoint;
	}

	public void setFixedPoint(Position fixedPoint) {
	    this.fixedPoint = fixedPoint;
	    makeTransform();
	}

	@Override
	public Position transform(Position position) {
	    return positionTransform.transform(position);
	}
	
    }
    

}
