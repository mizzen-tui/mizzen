package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;

/**
 * Draws a line between 2 points
 */
public class LinePlotter {

    public void draw(int x1, int y1, int x2, int y2, PointPlotter plot) {
        int xdiff = Math.max(x1, x2) - Math.min(x1, x2);
        int ydiff = Math.max(y1, y2) - Math.min(y1, y2);
        int xdir = (x1 <= x2) ? 1 : -1;
        int ydir = (y1 <= y2) ? 1 : -1;

        int r = Math.max(xdiff, ydiff);

        // for (int i=0; i<= r+1; i++) { // overshoots by one
        for (int i = 0; i < r + 1; i++) {
            int x = x1;
            int y = y1;
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
