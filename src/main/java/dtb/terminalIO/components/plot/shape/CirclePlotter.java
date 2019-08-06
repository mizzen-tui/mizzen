package dtb.terminalIO.components.plot.shape;

import java.util.Comparator;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.shape.Circle.DrawCircleAlgorithm;
import dtb.terminalIO.components.plot.shape.math.TrigUtil;
import dtb.terminalIO.geom.Position;

/**
 * Draws a circle using specified {@link DrawCircleAlgorithm}
 */
public class CirclePlotter implements DrawCircleAlgorithm {
    
    @FunctionalInterface
    static interface PlotCondition {
        boolean test(int x, int y, double radius, double distance);
    }
    
    private PlotCondition plotCondition;

    public CirclePlotter(PlotCondition plotCondition) {
        super();
        this.plotCondition = plotCondition;
    }

    public CirclePlotter(boolean filled) {
        super();
        if (filled) {
            plotCondition = (x, y, radius, distance)-> (compare(radius, distance, 0.5d) >= 0);
        } else {
            plotCondition = (x, y, radius, distance)-> (compare(radius, distance, 0.5d) == 0);
        }
    }
    
    public CirclePlotter() {
        this(false);
    }

    public void draw(int x, int y, int radius, PointPlotter plot) {
        // yeah .. yeah .. seems so inefficient to scan all points in the square
        //                 but against all my better instincts .. it works pretty fast
        for (int x1 = x - radius; x1 <= x + radius; x1++) {
            for (int y1 = y - radius; y1 <= y + radius; y1++) {
                double distance = Math.sqrt(Math.pow(x - x1, 2) + Math.pow(y - y1, 2));
                if (plotCondition.test(x1,y1,(double)radius, distance))
                    plot.plot(x1, y1);
            }
        }
    }

    static protected int compare(double d1, double d2, double precision) {
        double diff = d1 - d2;
        if (Math.abs(diff) < precision)
            return 0;
        if (diff < 0d)
            return -1;
        else
            return 1;
    }

    /**
     * A {@link DrawCircleAlgorithm} that plots a circle and then an inner circle so  that it
     * ends up looking like a donut.
     * 
     * 
     * <pre>
     * 
     *  ASCII ( non braille ) rendering of a donut, filled = false
     * 
     * 
     *                ._/-----__ 
     *            ./r+`         -*c_ 
     *          *r`                 \\.
     *        .)`                     +\
     *       :+        _-/----\.       '\
     *      .[       //        '\.      '|
     *      *       /            ::      :.
     *      |      |`             |       |
     *      |      *              *       |
     *      \       |            .|      ::
     *      ::      '\.         //       /
     *       7:       '--\___-//        j`
     *        :\                      .)
     *         '\c                  ./+
     *            \\c.           _+-`
     *               -\-\--------`
     * 
     * </pre>
     * 
     * 
     * If filled is true - fills in the area between the 2 circles
     * 
     * <pre>
     * 
     *  ASCII ( non braille ) rendering of a donut, filled = true
     * 
     *                ._******__
     *            .**############**_
     *          **##################**.
     *        .d#######################c
     *       :###########P"""#@#########*
     *      .#########+        '9########L
     *      *#######P            :########.
     *      ########`             T#######|
     *      ########              *#######|
     *      ?#######L            .########:
     *      :########*.         *########P
     *       7#########**c___*d##########`
     *        :@#######################P
     *         '9#####################+
     *            +9###############"`
     *               -+"9#####""-`
     * 
     * </pre>
     * 
     */
    public static class DonutPlotter implements DrawCircleAlgorithm {
        
        boolean filled;
        Integer startAngle;
        Integer endAngle;
        
        
        public DonutPlotter(boolean filled) {
            this.filled = filled;
        }

        public DonutPlotter(int startAngle, int endAngle) {
            setAngles(startAngle, endAngle);
        }

        public DonutPlotter(int endAngle) {
            setAngle(endAngle);
        }
        
        public DonutPlotter setAngle(int endAngle) {
            return setAngles(0, endAngle);
        }

        public int getEndAngle() {
            return endAngle;
        }

        public int getStartAngle() {
            return startAngle;
        }

        public DonutPlotter setAngles(int startAngle, int endAngle) {
            checkAngle(startAngle);
            checkAngle(endAngle);
            this.endAngle = endAngle;
            this.startAngle = startAngle;
            this.filled = true;
            return this;
        }
        
        private void checkAngle(int angle) {
            if (angle<0 || angle > 360) throw new IllegalArgumentException("angle of range! "+angle+ " not in ( 0 .. 360 )");
        }
        
        public void draw(int x, int y, int outerRadius, PointPlotter plot) {

            int innerRadius = outerRadius/2;
            PlotCondition plotCondition;
            if (filled) {
                plotCondition = (x1, y1, radius, distance) -> {
                    return  compare(radius, distance, 0.5d) >= 0 &&
                            compare(innerRadius, distance, 0.5d) <= 0;
                };

                if (endAngle != null) {
                    if (startAngle == null) {
                        startAngle = 0;
                    }
                    plotCondition = (x1, y1, radius, distance) -> {
                        boolean inDonut =   (compare(radius, distance, 0.5d) >= 0 &&
                                             compare(innerRadius, distance, 0.5d) <= 0);
                        if (!inDonut) return false;
                        int curAngle = TrigUtil.circleAngleAtPoint(x1, y1, x, y);
                        if (startAngle <= endAngle) {
                            if (curAngle >= startAngle && curAngle<=endAngle) return true;
                        } else {
                            if (curAngle >= startAngle || curAngle<=endAngle) return true;
                        }
                        return false;
                    };
                }
                
            } else {
                plotCondition = (x1, y1, radius, distance) -> {
                     return compare(radius, distance, 0.5d) == 0 ||
                            compare(innerRadius, distance, 0.5d) == 0;
                };
            }
            CirclePlotter plotter = new CirclePlotter(plotCondition);
            plotter.draw(x, y, outerRadius, plot);
        }
    }
    
    
    public static class CircleClockwise_SORT implements Comparator<Position> {

        int center_x;
        int center_y;

        public CircleClockwise_SORT(int center_x, int center_y) {
            super();
            this.center_x = center_x;
            this.center_y = center_y;
        }

        private int calcQuadrant(int x, int y) {
            if (y <= center_y) {
                return x <= center_x ? 0 : 1;
            }
            return x <= center_x ? 3 : 2;
        }

        @Override
        public int compare(Position o1, Position o2) {

            int x1 = o1.getCol();
            int y1 = o1.getRow();
            int x2 = o2.getCol();
            int y2 = o2.getRow();

            int q1 = calcQuadrant(x1, y1);
            int q2 = calcQuadrant(x2, y2);

            int cmp = Integer.compare(q1, q2);

            if (cmp != 0)
                return cmp;

            switch (q1) {
            case 0:
                cmp = Integer.compare(x1, x2);
                return cmp != 0 ? cmp : Integer.compare(y2, y1);
            case 1:
                cmp = Integer.compare(x1, x2);
                return cmp != 0 ? cmp : Integer.compare(y1, y2);
            case 2:
                cmp = Integer.compare(x2, x1);
                return cmp != 0 ? cmp : Integer.compare(y1, y2);
            case 3:
                cmp = Integer.compare(x2, x1);
                return cmp != 0 ? cmp : Integer.compare(y2, y1);

            default:
                return 0;
            }
        }
    }

    public static class CircleCounterClockwise_SORT implements Comparator<Position> {
        CircleClockwise_SORT clockwise;

        public CircleCounterClockwise_SORT(int center_x, int center_y) {
            this.clockwise = new CircleClockwise_SORT(center_x, center_y);
        }

        @Override
        public int compare(Position o1, Position o2) {
            return clockwise.compare(o1, o2) * -1;
        }

    }

}
