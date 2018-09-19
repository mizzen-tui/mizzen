package dtb.terminalIO.components.plot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Position;


public interface PointPlotter {
    
    void plot(int x, int y);
    Color setColor(Color color);

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

	@Override
	public Color setColor(Color color) {
	    return this.getPointPlotter().setColor(color);
	}

//	@Override
//	public void plot(int x, int y, Color color) {
//	    this.getPointPlotter().plot(x, y, color);
//	    
//	}

    }
    
    public static class PointPlot implements Position  {
	public int x;
	public int y;
	public Color color;

	public PointPlot(int x, int y, Color color) {
	    super();
	    this.x = x;
	    this.y = y;
	    this.color = color;
	}
	
	public PointPlot(int x, int y) {
	    this(x,y, null);
	}
	
	public PointPlot(Position p, Color color) {
	    this(p.getCol(), p.getRow(), color);
	}

	public PointPlot(Position p) {
	    this(p, null);
	}
	
	
	@Override
	public int getCol() {
	    return x;
	}
	@Override
	public int getRow() {
	    return y;
	}
	
    }
    
    /**
     * A list contains plots ( a plot = a Position object with optional color)
     */
    public static class PointPlotterList implements PointPlotter {
	
	List<PointPlot> plots;
	Color currentColor=null;
	
	public PointPlotterList() {
	    super();
	    this.plots = new ArrayList<PointPlot>();
	}

	public List<PointPlot> getPlots() {
	    return plots;
	}

	@Override
	public void plot(int x, int y) {
	    getPlots().add( new PointPlot(y,x));
	}

	public void plot(int x, int y, Color color) {
	    getPlots().add( new PointPlot(y,x, color!=null?color:currentColor));
	}

	public int size() {
	    return this.plots==null? 0 : this.plots.size();
	}
	
	public void sort(Comparator<Position> sortComparator) {
	    Collections.sort(getPlots(), sortComparator);
	}

	@Override
	public Color setColor(Color color) {
	    Color prevColor = currentColor;
	    currentColor = color;
	    return prevColor;
	}

	
	
    }

    
}