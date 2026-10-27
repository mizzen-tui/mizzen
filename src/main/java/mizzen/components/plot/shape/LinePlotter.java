package mizzen.components.plot.shape;

import mizzen.components.plot.PointPlotter;

/**
 * Draws a line between 2 points into a {@link PointPlotter}.
 */
public class LinePlotter {

    public void draw(float x1, float y1, float x2, float y2, PointPlotter plot) {
        float xdiff = Math.max(x1, x2) - Math.min(x1, x2);
        float ydiff = Math.max(y1, y2) - Math.min(y1, y2);
        float xdir = (x1 <= x2) ? 1 : -1;
        float ydir = (y1 <= y2) ? 1 : -1;

        float r = Math.max(xdiff, ydiff);

        // for (int i=0; i<= r+1; i++) { // overshoots by one
        for (int i = 0; i < r + 1; i++) {
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
}
