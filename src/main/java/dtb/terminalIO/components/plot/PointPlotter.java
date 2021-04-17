package dtb.terminalIO.components.plot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.position.Position;

/**
 * 
 * A container ( of sorts ) that allows you to plot x, y
 * coordinates ( with optional color ) onto some destination
 * - usually a grid component ..  
 * 
 * <p/>
 * 
 * But it could be plotted simply to memory (like to a
 * PointPlotterList). 
 * 
 * @author dbalme
 *
 */
public interface PointPlotter {

    void plot(float x, float y);
    
    default void plot(PointPlot p) {
        plot(p.x, p.y);
    }

    default void plot(Position p) {
        plot(p.getCol(), p.getRow());
    }
    
    
    Color setNextPlotColor(Color color);
    
    void clearPlots();

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
        public void plot(float x, float y) {
            this.getPointPlotter().plot(x, y);
        }

        @Override
        public Color setNextPlotColor(Color color) {
            return this.getPointPlotter().setNextPlotColor(color);
        }

        @Override
        public void clearPlots() {
            this.getPointPlotter().clearPlots();
        }

//	@Override
//	public void plot(int x, int y, Color color) {
//	    this.getPointPlotter().plot(x, y, color);
//	    
//	}

    }

    /**
     * Same thing as a {@link Position} object
     * but we just refer to col() as x and row() as y
     * 
     * <p/>
     * 
     * It also optionally maintains a Color.
     * 
     * <p/>
     * 
     * This is used when plotting points on a Grid.
     * 
     * 
     * @author dbalme
     *
     */
    public static class PointPlot implements Position {
        
        public float x;
        public float y;
        public Color color;

        public PointPlot(float x, float y, Color color) {
            super();
            this.x = x;
            this.y = y;
            this.color = color;
        }
        
        public PointPlot(int x, int y, Color color) {
            this((float) x, (float) y, color);
        }

        public PointPlot(float x, float y) {
            this(x, y, null);
        }

        public PointPlot(int x, int y) {
            this(x, y, null);
        }

        public PointPlot(Position p, Color color) {
            this(p.getCol(), p.getRow(), color);
        }

        public PointPlot(Position p) {
            this(p, null);
        }

        @Override
        public int getCol() {
            return (int)x;  // use Math.round()?
        }

        @Override
        public int getRow() {
            return (int)y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
        
        

    }

    /**
     * A {@link PointPlotter} that plots points to list rather than
     * to a grid of some sort.
     * 
     * <p/>
     * 
     * See its use in PathPlotter
     * 
     * <p/>
     * 
     * A list contains plots ( a plot = a Position object with optional color)
     */
    public static class PointPlotterList implements PointPlotter {

        List<PointPlot> plots;
        Color currentColor = null;

        public PointPlotterList() {
            super();
            this.plots = new ArrayList<PointPlot>();
        }

        public List<PointPlot> getPlots() {
            return plots!=null? plots : Collections.emptyList();
        }

        @Override
        public void plot(float x, float y) {
            plot(x, y, currentColor);
//            getPlots().add(new PointPlot(y, x));
        }

        public void plot(float x, float y, Color color) {
            getPlots().add(new PointPlot(x, y, color != null ? color : currentColor));
        }

        public int size() {
            return this.plots == null ? 0 : this.plots.size();
        }

        public void sort(Comparator<Position> sortComparator) {
            Collections.sort(getPlots(), sortComparator);
        }

        @Override
        public Color setNextPlotColor(Color color) {
            Color prevColor = currentColor;
            currentColor = color;
            return prevColor;
        }

        @Override
        public void clearPlots() {
            getPlots().clear();
        }
        
        public void addAll(PointPlotterList other) {
            plots.addAll(other.getPlots());
        }

    }

}