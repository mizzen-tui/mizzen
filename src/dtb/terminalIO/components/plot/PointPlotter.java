package dtb.terminalIO.components.plot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dtb.terminalIO.geom.Position;

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
    
    public static class PointPlotterList implements PointPlotter {
	
	List<Position> plots;
	
	public PointPlotterList() {
	    super();
	    this.plots = new ArrayList<Position>();
	}

	public List<Position> getPlots() {
	    return plots;
	}

	public void setPlots(List<Position> plots) {
	    this.plots = plots;
	}

	@Override
	public void plot(int x, int y) {
	    getPlots().add( new Position.Simple(y,x));
	}

	public int size() {
	    return this.plots==null? 0 : this.plots.size();
	}
	
	public void sort(Comparator<Position> sortComparator) {
	    Collections.sort(getPlots(), sortComparator);
	}
	
	
    }
    
}