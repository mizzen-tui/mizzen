package dtb.terminalIO.components.plot.shape;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.components.plot.shape.Circle.DrawCircleAlgorithm;
import dtb.terminalIO.components.plot.shape.math.TrigUtil;
import dtb.terminalIO.geom.position.Position;

/**
 * Draws a circle using specified {@link DrawCircleAlgorithm}
 */
public class CircleAlgorithm implements DrawCircleAlgorithm {
    
    @FunctionalInterface
    static interface CirclePlotCondition {
        boolean test(int x, int y, double radius, double distance, int midX, int midY);
        
        default CirclePlotCondition and(CirclePlotCondition other) {
            Objects.requireNonNull(other);
            return (int x, int y, double radius, double distance, int midX, int midY) -> {
                return test(x, y, radius, distance, midX, midY) && 
                       other.test(x, y, radius, distance, midX, midY);
                };
        }        
    }
    
    static class CirclePlotConditionDelegate implements CirclePlotCondition {
        CirclePlotCondition delegate;

        public CirclePlotConditionDelegate(CirclePlotCondition delegate) {
            super();
            this.delegate = delegate;
        }

        public CirclePlotCondition getDelegate() {
            return delegate;
        }

        public void setDelegate(CirclePlotCondition delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean test(int x, int y, double radius, double distance, int midX, int midY) {
            return delegate.test(x, y, radius, distance, midX, midY);
        }
    }

    private CirclePlotCondition plotCondition;

    public CircleAlgorithm(CirclePlotCondition plotCondition) {
        super();
        this.plotCondition = plotCondition;
    }

    public CircleAlgorithm(boolean filled) {
        super();
        if (filled) {
            plotCondition = plotConditionFilled();
        } else {
            plotCondition = plotConditionNotFilled();
        }
    }

    public CircleAlgorithm() {
        this(false);
    }

    public void draw(int midX, int midY, int radius, PointPlotter plot) {
        // yeah .. yeah .. seems so inefficient to scan all points in the square
        //                 but against all my better instincts .. it works pretty fast
        for (int x1 = midX - radius; x1 <= midX + radius; x1++) {
            int xDiffSquared = (midX - x1)*(midX - x1);
            for (int y1 = midY - radius; y1 <= midY + radius; y1++) {
                double distance = Math.sqrt(xDiffSquared + Math.pow(midY - y1, 2));
                // double distanceSquared = xDiffSquared + (y - y1)*(y - y1);
                if (plotCondition.test(x1,y1, radius, distance, midX, midY)) {
                    plot.plot(x1, y1);
                }
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

    static int shift90(int angle) {
        // shift 90 degrees
        return (angle + 90) % 360;
    }

    public static class ConditionBetweenAngles implements CirclePlotCondition {
        int startAngle;
        int endAngle;
        public ConditionBetweenAngles(int startAngle, int endAngle) {
            super();
            this.startAngle = startAngle;
            this.endAngle = endAngle;
        }

        public boolean test(int x1, int y1, double radius, double distance, int midX, int midY) {
            double curAngle = TrigUtil.circleAngleAtPoint(x1, y1, midX, midY);
            if (startAngle <= endAngle) {
                if (curAngle >= startAngle && curAngle<=endAngle) return true;
            } else {
                if (curAngle >= startAngle || curAngle<=endAngle) return true;
            }
            return false;
        }
    }

    public static class ConditionOutsideOfAngles implements CirclePlotCondition {
        ConditionBetweenAngles conditionBetweenAngles;
        public ConditionOutsideOfAngles(int a1InDegrees, int a2InDegrees) {
            super();
            this.conditionBetweenAngles = new ConditionBetweenAngles(a1InDegrees, a2InDegrees);
        }

        public boolean test(int x1, int y1, double radius, double distance, int midX, int midY) {
            return ! conditionBetweenAngles.test(x1, y1, radius, distance, midX, midY);
        }
    }

    public static CirclePlotCondition plotConditionNotFilled() {
        CirclePlotCondition plotCondition = (x, y, radius, distance, midX, midY)-> (compare(radius, distance, 0.5d) == 0);
        return plotCondition;
    }

    public static CirclePlotCondition plotConditionFilled() {
        CirclePlotCondition plotCondition = (x, y, radius, distance, midX, midY)-> (compare(radius, distance, 0.5d) >= 0);
        return plotCondition;
    }

    public static CirclePlotCondition basicPlotConditionFilled(boolean filled) {
        CirclePlotCondition plotCondition;
        if (filled) {
            plotCondition = plotConditionFilled();
        } else {
            plotCondition = plotConditionNotFilled();
        }
        return plotCondition;
    }

    public static CirclePlotCondition plotConditionBetweenAngles(int a1InDegrees, int a2InDegrees) {
        return plotConditionBetweenAngles(a1InDegrees, a2InDegrees, false);
    }

    public static CirclePlotCondition plotConditionBetweenAngles(int a1InDegrees, int a2InDegrees, boolean filled) {
        return basicPlotConditionFilled(filled).and( new ConditionBetweenAngles(a1InDegrees, a2InDegrees));
    }

    public static CirclePlotCondition plotConditionOutsideOfAngles(int a1InDegrees, int a2InDegrees) {
        return plotConditionOutsideOfAngles(a1InDegrees, a2InDegrees, false);
    }

    public static CirclePlotCondition plotConditionOutsideOfAngles(int a1InDegrees, int a2InDegrees, boolean filled) {
        return basicPlotConditionFilled(filled).and(new ConditionOutsideOfAngles(a1InDegrees, a2InDegrees));
    }

    public static CirclePlotCondition plotConditionNotIntersectingOther(Circle other, boolean filled) {
        CirclePlotCondition baseCondition = basicPlotConditionFilled(filled);
        CirclePlotCondition intersectCondition = basicPlotConditionFilled(true);
        CirclePlotCondition plotCondition = (x, y, radius, distance, midX, midY)-> {
            if ( ! baseCondition.test(x, y, radius, distance, midX, midY)) {
                return false;
            }
            double otherRadius = other.getPositionTransform() != null
                                                  ? other.getTransformedRadius(other.getRadius(), other.getPositionTransform())  
                                                  : other.getRadius();
            PointPlot otherMidPoint = other.getMidPoint();
            if (other.getPositionTransform() != null) {
                otherMidPoint = new PointPlot(other.getPositionTransform().transform(otherMidPoint));
            }
            int otherMidX = Math.round(otherMidPoint.getX());
            int otherMidY = Math.round(otherMidPoint.getY());
            double otherDistance = Math.sqrt(Math.pow(otherMidX - x, 2) + Math.pow(otherMidY - y, 2));
            if (intersectCondition.test(x, y, otherRadius, otherDistance, otherMidX, otherMidY)) {
                return false;
            }
            return true;
        };
        return plotCondition;
    }

    /**
     * A {@link DrawCircleAlgorithm} that plots a circle and then an inner circle so that it
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
    public static class DonutCircleAlgorithm implements DrawCircleAlgorithm {

        boolean filled;
        Integer startAngle;
        Integer endAngle;

        public DonutCircleAlgorithm(boolean filled) {
            this.filled = filled;
        }

        public DonutCircleAlgorithm(int startAngle, int endAngle) {
            setAngles(startAngle, endAngle);
        }

        public DonutCircleAlgorithm(int endAngle) {
            setAngle(endAngle);
        }

        public DonutCircleAlgorithm setAngle(int endAngle) {
            return setAngles(0, endAngle);
        }

        public int getEndAngle() {
            return endAngle;
        }

        public int getStartAngle() {
            return startAngle;
        }

        public DonutCircleAlgorithm setAngles(int startAngle, int endAngle) {
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

        public void draw(int midX, int midY, int outerRadius, PointPlotter plot) {

            float innerRadius = outerRadius/2;
            CirclePlotCondition plotCondition;
            if (filled) {
                plotCondition = (x1, y1, radius, distance, centerX, centerY) -> {
                    return  compare(radius, distance, 0.5d) >= 0 &&
                            compare(innerRadius, distance, 0.5d) <= 0;
                };

                if (endAngle != null) {
                    if (startAngle == null) {
                        startAngle = 0;
                    }
                    plotCondition = (x1, y1, radius, distance, centerX, centerY) -> {
                        boolean inDonut =   (compare(radius, distance, 0.5d) >= 0 &&
                                             compare(innerRadius, distance, 0.5d) <= 0);
                        if (!inDonut) return false;
                        double curAngle = TrigUtil.circleAngleAtPoint(x1, y1, centerX, centerY);
                        if (startAngle <= endAngle) {
                            if (curAngle >= startAngle && curAngle<=endAngle) return true;
                        } else {
                            if (curAngle >= startAngle || curAngle<=endAngle) return true;
                        }
                        return false;
                    };
                }
                
            } else {
                plotCondition = (x1, y1, radius, distance, centerX, centerY) -> {
                     return compare(radius, distance, 0.5d) == 0 ||
                            compare(innerRadius, distance, 0.5d) == 0;
                };
            }
            CircleAlgorithm plotter = new CircleAlgorithm(plotCondition);
            plotter.draw(midX, midY, outerRadius, plot);
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
