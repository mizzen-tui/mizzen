package dtb.terminalIO.components.plot;

import java.util.TreeSet;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.CleanString;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;

/**
 * A component that can plot a series of dots .. Useful sub-component for
 * charting components
 */
public class Plotter extends BoundedComponent<Plotter> {
    // todo: 1) addPlot should reject (either silently or noisily) any plots
    // outside of the bounds
    // 2) add draw line - using algorithm - maybe not in this class - but something
    // that controls this class
    // 3) draw box
    // 4 draw circle

    char defaultPlotChar = '*';

    TreeSet<Plotter.Plot> plots = new TreeSet<Plotter.Plot>(Position.UTIL.POS_COMPARE);

    public Plotter(Color color, Dimension d) {
        super(color, d);
        this.setFillOnClear(false);
    }

    public Plotter(Color color, int height, int width) {
        this(color, new Dimension.Simple(height, width));
    }

    public Plotter addPlot(int row, int col, char plotChar, Color color) {
        if (!this.containsPosition(row, col)) {
            // throw new IllegalArgumentException(String.format("Bad plot! row %d %d not in
            // range %s", row, col, String.valueOf(this)));
            color = new Colors.FGBGColors(Colors.IRed);
        }
        plots.add(new Plot(row, col, plotChar, color));
        return this;
    }

    public Plotter addPlot(Plot plot) {
        if (!this.containsPosition(plot.getRow(), plot.getCol())) {
            // throw new IllegalArgumentException(String.format("Bad plot! row %d %d not in
            // range %s", row, col, String.valueOf(this)));
            Color color = new Colors.FGBGColors(Colors.IRed);
            plot.setPlotColor(color);
        }
        plots.add(plot);
        return this;
    }

    public Plotter addPlot(int row, int col, char plotChar) {
        return addPlot(row, col, plotChar, this.getColor());

    }

    public Plotter addPlot(int row, int col) {
        return addPlot(row, col, defaultPlotChar, this.getColor());
    }

    public Plotter addText(int row, int col, CleanString text, Color color) {
        int sLen = text.length();
        for (int i = 0; i < sLen; i++) {
            addPlot(row, col+i, text.charAt(i));
        }
        return this;
    }

    public Plotter addText(int row, int col, CleanString text) {
        return addText(row, col, text);
    }

//    protected void drawOLD(Color color, IScreenLineSegmentsBuffer segments) {
//
//        for (Plotter.Plot plot : plots) {
//            segments.setCharAt(plot.pos.getRow(), plot.pos.getCol(), plot.plotChar, plot.plotColor);
//        }
//    }

    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        
        long elapsed = ElapsedTime.onRunOf(()->{
            
            __draw(color, segments);
            
        }).elapsed();
        
        Logging.info("Plotter["+this.getName()+"].draw() - elapsed: " + elapsed +" size: " + segments.getSegments().size()+ " plotsSize="+plots.size());
    }
    
    private void __draw(Color color, IScreenLineSegmentsBuffer segments) {

        int lastRow = -1;
        int lastCol = -1;

        SegmentImpl lastSegment = null;
        Position    lastSegmentPosition = null;

        Color lastColor = Colors.DEFAULT_COLOR;

        StringBuilder lastText = new StringBuilder();

        // We must translate all plots into Segments.
        //
        // NOTE:
        //   Each plot object only contains one character to plot.
        //   To reduce the number of created Segments, adjacent, same colored plots
        //   will be translated into one Segment.  This creates rendering efficiencies "down-the-road"
        
        // Because we are building ScreenLineSegment's that do not overlap, we will use the fast TreeSet add() methods
        // to build the tree of segments.
        TreeSet<ScreenLineSegment> treeSetOfSegments = segments.getSegments();
        

        for (Plotter.Plot plot : plots) {
            if (lastRow != plot.pos.getRow()     || 
                !plot.plotColor.equals(lastColor)||
                plot.pos.getCol() != lastCol+1) {
                if (lastText.length() != 0) {
                    lastSegment = new SegmentImpl(lastColor, lastSegmentPosition, -1, lastText.toString());
                    treeSetOfSegments.add(lastSegment);
                    lastSegment = null;
                    lastText = new StringBuilder();
                }
                lastColor = plot.plotColor;
                lastRow = plot.pos.getRow();
                lastSegmentPosition = plot.pos;
            }
            lastText.append(plot.plotChar);
            lastCol = plot.pos.getCol();
        }

        if (lastText.length() != 0) {
            lastSegment = new SegmentImpl(lastColor, lastSegmentPosition, -1, lastText.toString());
            treeSetOfSegments.add(lastSegment);
        }

    }

    public void clearPlots() {
        plots.clear();
    }

    public String getCharAt(int row, int col) {
        Plot srchKey = new Plot(new Position.Simple(row, col), (char) 0, Colors.FGBGColors.NO_COLOR_SPECIFIED);
        Plot found = plots.floor(srchKey);
        if (found == null || Position.UTIL.POS_COMPARE.compare(srchKey, found) != 0) {
            return null;
        }
        return found.plotChar + "";
    }

    public Plot getPlotAt(int row, int col) {
        Plot srchKey = new Plot(new Position.Simple(row, col), (char) 0, Colors.FGBGColors.NO_COLOR_SPECIFIED);
        Plot found = plots.floor(srchKey);
        if (found == null || Position.UTIL.POS_COMPARE.compare(srchKey, found) != 0) {
            return null;
        }
        return found;
    }

    public PointPlotter asPointPlotter() {
        PointPlotter plot = new PointPlotter() {
            @Override
            public void plot(int x, int y) {
                addPlot(y, x);
            }

            @Override
            public Color setNextPlotColor(Color color) {
                Color prevColor = Plotter.this.getColor();
                Plotter.this.setColor(color);
                return prevColor;
            }

            @Override
            public void clearPlots() {
                Plotter.this.clearPlots();
            }

        };
        return plot;
    }
    
    public static class Plot implements Position {
        public Position pos;
        public char plotChar;
        public Color plotColor;

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