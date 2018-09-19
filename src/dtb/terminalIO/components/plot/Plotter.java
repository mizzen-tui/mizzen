package dtb.terminalIO.components.plot;

import java.util.TreeSet;

import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;

/**
 * A component that can plot a series of dots ..  Useful sub-component for charting components
 */
public class Plotter extends BoundedComponent<Plotter> {
    // todo: 1) addPlot should reject (either silently or noisily) any plots
    //          outside of the bounds
    //       2) add draw line - using algorithm - maybe not in this class - but something that controls this class
    //       3) draw box
    //       4  draw circle

    char defaultPlotChar = '*';
    
    TreeSet<Plotter.Plot> plots = new TreeSet<Plotter.Plot>(Position.UTIL.POS_COMPARE);
    
    public Plotter(Color color, Dimension d) {
        super(color, d);
    }

    public Plotter(Color color, int row, int height) {
        this(color, new Dimension.Simple(row, height));
    }

    public Plotter addPlot(int row, int col, char plotChar, Color color) {
        if ( ! this.containsPosition(new Position.Simple(row,col))) {
            //throw new IllegalArgumentException(String.format("Bad plot!  row %d %d not in range %s", row, col, String.valueOf(this)));
            color = new FGBGColors(Colors.IRed);
        }
        plots.add( new Plot(row, col, plotChar, color) );
        return this;
    }
    
    public Plotter addPlot(int row, int col, char plotChar) {
        return addPlot(row, col, plotChar, this.getColor());

    }
    public Plotter addPlot(int row, int col) {
        return addPlot(row, col, defaultPlotChar, this.getColor());
    }
    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        
        for (Plotter.Plot plot : plots) {
            segments.setCharAt(plot.pos.getRow(), plot.pos.getCol(), plot.plotChar, plot.plotColor);
        }
    }
    
    public void clearPlots() {
        plots.clear();
    }

    public String getCharAt(int row, int col) {
	Plot srchKey = new Plot(new Position.Simple(row, col), (char)0, FGBGColors.NO_COLOR_SPECIFIED);
	Plot found = plots.floor(srchKey);
	if (found == null || Position.UTIL.POS_COMPARE.compare(srchKey, found) != 0) {
	    return null;
	}
	return found.plotChar+"";
    }

    public Plot getPlotAt(int row, int col) {
	Plot srchKey = new Plot(new Position.Simple(row, col), (char)0, FGBGColors.NO_COLOR_SPECIFIED);
	Plot found = plots.floor(srchKey);
	if (found == null || Position.UTIL.POS_COMPARE.compare(srchKey, found) != 0) {
	    return null;
	}
	return found;
    }

    public static class Plot implements Position {
        Position pos;
        char plotChar;
        Color plotColor;
        public Plot(Position pos, char plotChar, Color plotColor) {
            super();
            this.pos = pos;
            this.plotChar = plotChar;
            this.plotColor = plotColor;
        }
        
        public Plot(int row, int col, char plotChar, Color plotColor) {
            this(new Position.Simple(row, col), plotChar, plotColor);
        }

	@Override
	public int getCol() {
	    return pos.getCol();
	}

	@Override
	public int getRow() {
	    return pos.getRow();
	}
	
	public char getPlotChar() {
	    return plotChar;
	}

	public void setPlotChar(char theCHAR) {
	    this.plotChar = theCHAR;
	}

	public Color getPlotColor() {
	    return plotColor;
	}

	public void setPlotColor(Color plotColor) {
	    this.plotColor = plotColor;
	}
        
    }
    
    
    
}