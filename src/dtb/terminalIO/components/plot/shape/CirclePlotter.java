package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;

public class CirclePlotter {
    
    public void draw(int x, int y, int radius, PointPlotter plot) {

	for (int x1 = x - radius; x1 <= x + radius; x1++) {
	    for (int y1 = y - radius; y1 <= y + radius; y1++) {
		double distance = Math.sqrt(Math.pow(x - x1, 2) + Math.pow(y - y1, 2));
		if (compare(radius, distance, 0.5d) == 0)
		    plot.plot(x1, y1);
	    }
	}
    }

    protected int compare(double d1, double d2, double precision) {
	double diff = d1 - d2;
	if (Math.abs(diff) < precision)
	    return 0;
	if (diff < 0d)
	    return -1;
	else
	    return 1;
    }

}
