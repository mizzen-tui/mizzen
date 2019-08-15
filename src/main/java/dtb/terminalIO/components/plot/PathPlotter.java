package dtb.terminalIO.components.plot;

import java.util.Comparator;

import dtb.terminalIO.components.plot.PointPlotter.PointPlotterList;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CircleAlgorithm.CircleClockwise_SORT;
import dtb.terminalIO.geom.Position;

/**
 * A PathPlotter draws shapes into a PointPlotterList..
 * 
 * <p/>
 * 
 * This is useful when you want to create a path that is in the form of a 
 * shape.   For example.. Maybe you have a box that you wan to move around
 * in a circular path.   You can do that with this type of object
 * 
 * <p/>
 * 
 * See uses of CirclePathPlotter for example uses.
 * 
 * @author dbalme
 *
 */
public abstract class PathPlotter {
    PointPlotter.PointPlotterList plotsInPath = new PointPlotter.PointPlotterList();

    public void plotOnePointFromPath() {
        if (plotsInPath.size() == 0) {
            buildPlotPath(plotsInPath);
        }
        Position curPos = plotsInPath.getPlots().get(0);

        plotsInPath.getPlots().remove(0);

        plotOnePoint(curPos);
    }

    abstract protected void buildPlotPath(PointPlotter.PointPlotterList plots);

    abstract protected void plotOnePoint(Position curPos);

    public static abstract class CirclePathPlotter extends PathPlotter {
        int x;
        int y;
        int radius;
        Comparator<Position> pointsSorter;

        public CirclePathPlotter(int x, int y, int radius, Comparator<Position> pointsSorter) {
            super();
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.pointsSorter = pointsSorter;

        }

        public CirclePathPlotter(int x, int y, int radius) {
            this(x, y, radius, new CircleClockwise_SORT(x, y));
        }

        protected void buildPlotPath(PointPlotterList plots) {
            Circle c = new Circle(x, y, radius);
            c.draw(plots);
            plots.sort(pointsSorter);
        }

    }

}