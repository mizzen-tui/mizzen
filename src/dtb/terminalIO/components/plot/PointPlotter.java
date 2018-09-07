package dtb.terminalIO.components.plot;

public interface PointPlotter {
    
    void plot(int x, int y);
    
    public static class Delegate implements PointPlotter {

	PointPlotter pointPlotter;
	
	public Delegate(PointPlotter pointPlotter) {
	    super();
	    this.pointPlotter = pointPlotter;
	}
	
	public PointPlotter getPointPlotter() {
	    return pointPlotter;
	}

	public void setPointPlotter(PointPlotter pointPlotter) {
	    this.pointPlotter = pointPlotter;
	}

	@Override
	public void plot(int x, int y) {
	    this.getPointPlotter().plot(x, y);
	}

    }
    
}