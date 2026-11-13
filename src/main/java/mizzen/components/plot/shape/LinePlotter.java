package mizzen.components.plot.shape;

import mizzen.components.plot.PointPlotter;
import mizzen.geom.Rectangle;

/**
 * Draws a line between 2 points into a {@link PointPlotter}: a point for every step of one along its longer side.
 * <p/>
 * When the plotter has a {@link PointPlotter#clip() clip}, only the steps inside it are taken, so the time a line takes
 * is at most the clip's size, however far off its ends are. A plot whose window is tiny next to its data (a flat series
 * fitted to a height of 0.000000001) puts a point billions of steps off the grid; stepped through, that froze the
 * event thread. The steps taken are the same ones as without the clip, so what shows doesn't change.
 */
public class LinePlotter {

    public void draw(float x1, float y1, float x2, float y2, PointPlotter plot) {
        float xdiff = Math.max(x1, x2) - Math.min(x1, x2);
        float ydiff = Math.max(y1, y2) - Math.min(y1, y2);
        float xdir = (x1 <= x2) ? 1 : -1;
        float ydir = (y1 <= y2) ? 1 : -1;

        float r = Math.max(xdiff, ydiff);
        if (Float.isNaN(r) || Float.isInfinite(r)) {
            return;                                               // no end to step to
        }

        long first = 0;
        long last = (long) Math.ceil(r + 1) - 1;                  // the steps i < r + 1, as before
        Rectangle clip = plot.clip();
        if (clip != null && r > 0) {
            double[] in = partInside(x1, y1, x2, y2, clip);
            if (in == null) {
                return;                                           // all of it outside
            }
            first = Math.max(first, (long) Math.floor(in[0] * r) - 1);    // a step to spare each side: rounding
            last = Math.min(last, (long) Math.ceil(in[1] * r) + 1);
        }

        if (r >= LONG) {
            // float can't count this far (16 million and on: its steps, and the line's own length, are thousands of dots
            // out), so the steps are worked out in double, from the ends as given
            double dx = (double) x2 - x1, dy = (double) y2 - y1;
            double steps = Math.max(Math.abs(dx), Math.abs(dy));
            first = 0;
            last = (long) Math.ceil(steps + 1) - 1;
            if (clip != null) {
                double[] in = partInside(x1, y1, x2, y2, clip);
                first = Math.max(first, (long) Math.floor(in[0] * steps) - 1);
                last = Math.min(last, (long) Math.ceil(in[1] * steps) + 1);
            }
            for (long i = first; i <= last; i++) {
                plot.plot((float) (x1 + i * dx / steps), (float) (y1 + i * dy / steps));
            }
            return;
        }
        for (long i = first; i <= last; i++) {
            float x = x1;
            float y = y1;
            if (ydiff > 0) {
                y += (((float) i) * ydiff) / r * ydir;
            }
            if (xdiff > 0) {
                x += (((float) i) * xdiff) / r * xdir;
            }
            plot.plot(x, y);
        }
    }

    private static final float LONG = 1 << 24;                    // steps a float counts exactly

    /**
     * The part of the line from (x1, y1) to (x2, y2) inside clip (widened by a point each side, for rounding), as the
     * fractions of the way along it where that part starts and ends; null: none of it is inside. The Liang-Barsky cut:
     * each edge of the clip trims the fractions from one end or the other.
     * <pre>
     *     from (0, 0) to (10, 0), clip columns 0..4: [0.0, 0.5] - wide 4 + 1 to spare
     * </pre>
     */
    static double[] partInside(double x1, double y1, double x2, double y2, Rectangle clip) {
        double left = clip.getCol() - 1, right = clip.getCol() + clip.getWidth();
        double top = clip.getRow() - 1, bottom = clip.getRow() + clip.getHeight();
        double dx = x2 - x1, dy = y2 - y1;
        double[] t = { 0, 1 };
        if (trim(-dx, x1 - left, t) && trim(dx, right - x1, t) && trim(-dy, y1 - top, t) && trim(dy, bottom - y1, t)) {
            return t;
        }
        return null;
    }

    // one edge: p is the line's pull towards it, q its room before it; false: the line is wholly outside this edge
    private static boolean trim(double p, double q, double[] t) {
        if (p == 0) {
            return q >= 0;                                        // parallel to the edge: in or out all along
        }
        double at = q / p;
        if (p < 0) {
            if (at > t[1]) {
                return false;
            }
            t[0] = Math.max(t[0], at);
        } else {
            if (at < t[0]) {
                return false;
            }
            t[1] = Math.min(t[1], at);
        }
        return true;
    }
}
