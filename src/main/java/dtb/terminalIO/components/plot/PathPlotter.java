package dtb.terminalIO.components.plot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import dtb.terminalIO.components.plot.PointPlotter.PointPlotterList;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CircleAlgorithm.CircleClockwise_SORT;
import dtb.terminalIO.components.plot.shape.Line;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.geom.position.Position;

/**
 * A PathPlotter draws shapes into a PointPlotterList..
 *
 * <p/>
 *
 * This is useful when you want to create a path that is in the form of a 
 * shape.   For example.. Maybe you have a box that you want to move around
 * in a circular path.   You can do that with this type of object
 *
 * <p/>
 *
 * See {@link CirclePathPlotter} for example uses.
 *
 * @author dbalme
 *
 */
public abstract class PathPlotter {
    PointPlotter.PointPlotterList plotsInPath = new PointPlotter.PointPlotterList();

    public Position nextPosition() {
        if (plotsInPath.size() == 0) {
            buildPlotPath(plotsInPath);
        }
        Position curPos = plotsInPath.getPlots().get(0);

        plotsInPath.getPlots().remove(0);
        
        return curPos;
    }

    public Position nextPosition(int skip) {
        for (int i = 0 ; i < skip; i++) {
            nextPosition();
        }
        return nextPosition();
    }
    
    abstract protected void buildPlotPath(PointPlotter.PointPlotterList plots);

    public static class ShapePathPlotter extends PathPlotter {
        
        private Shape<?> shape;

        public ShapePathPlotter(Shape<?> shape) {
            this.shape = shape;
        }
        
        protected void buildPlotPath(PointPlotterList plots) {
            shape.draw(plots);
        }

    }

    public static class CirclePathPlotter extends ShapePathPlotter {
        Comparator<Position> pointsSorter;

        public CirclePathPlotter(Circle circle, Comparator<Position> pointsSorter) {
            super(circle);
            this.pointsSorter = pointsSorter;
        }

        public CirclePathPlotter(int x, int y, int radius, Comparator<Position> pointsSorter) {
            this(new Circle(x, y, radius), pointsSorter);
            this.pointsSorter = pointsSorter;

        }

        public CirclePathPlotter(int x, int y, int radius) {
            this(x, y, radius, new CircleClockwise_SORT(x, y));
        }

        protected void buildPlotPath(PointPlotterList plots) {
            super.buildPlotPath(plots);
            plots.sort(pointsSorter);
        }

    }
    

    public static class LinePathPlotter extends ShapePathPlotter {

        public LinePathPlotter(Line line) {
            super(line);
        }

        public LinePathPlotter(int x1, int y1, int x2, int y2) {
           this(new Line(x1, y1, x2, y2));
        }

    }

    public static class TrianglePathPlotter extends ShapePathPlotter {
        
        public TrianglePathPlotter(Triangle triangle) {
            super(triangle);
        }
    }

    public static class CompositePathPlotter extends PathPlotter {
        
        Collection<PathPlotter> pathPlotters = new ArrayList<PathPlotter>();
        
        public CompositePathPlotter addPathPlotter(PathPlotter pathPlotter) {
            pathPlotters.add(pathPlotter);
            return this;
        }

        public CompositePathPlotter removeShape(PathPlotter pathPlotter) {
            pathPlotters.remove(pathPlotter);
            return this;
        }

        @Override
        protected void buildPlotPath(PointPlotterList plots) {
            PointPlotterList intermediate = new PointPlotterList();
            for (PathPlotter pathPlotter : pathPlotters) {
                intermediate.clearPlots();
                pathPlotter.buildPlotPath(intermediate);
                plots.addAll(intermediate);
            }
        }
    }


}