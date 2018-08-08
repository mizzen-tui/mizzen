package dtb.terminalIO.components.chart;

import java.util.ArrayList;
import java.util.Collection;

import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;

public class Plotter extends BoundedComponent<Plotter> {
    // todo: 1) addPlot should reject (either silently or noisily) and plots
    //          outside of the bounds
    //       2) add draw line - using algorithm - maybe not in this class - but something that controls this class
    //       3) draw box
    //       4  draw circle

    
    char defaultPlotChar = '*';
    
    Collection<Plotter.Plot> plots = new ArrayList<Plotter.Plot>();
    
    public Plotter(FGBGColors color, Dimension d) {
        super(color, d);
    }

    public Plotter(FGBGColors color, int row, int height) {
        this(color, new Dimension.Simple(row, height));
    }

    public Plotter addPlot(int row, int col, char plotChar, FGBGColors color) {
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
    protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
        
        for (Plotter.Plot plot : plots) {
            segments.setCharAt(plot.pos.getRow(), plot.pos.getCol(), plot.plotChar, plot.plotColor);
        }
    }
    
    public void clearPlots() {
        plots.clear();
    }


    public static class Plot {
        Position pos;
        char plotChar;
        FGBGColors plotColor;
        public Plot(Position pos, char plotChar, FGBGColors plotColor) {
            super();
            this.pos = pos;
            this.plotChar = plotChar;
            this.plotColor = plotColor;
        }
        
        public Plot(int row, int col, char plotChar, FGBGColors plotColor) {
            this(new Position.Simple(row, col), plotChar, plotColor);
        }
        
    }
    
    
}