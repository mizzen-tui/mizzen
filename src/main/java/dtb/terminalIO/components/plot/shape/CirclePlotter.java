package dtb.terminalIO.components.plot.shape;

import java.util.Comparator;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.Position;

/**
 * Draws a circle
 *
 */
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
    
    
    public static class CircleClockwise_SORT implements Comparator<Position> {
	
	int center_x;
	int center_y;
	
	public CircleClockwise_SORT(int center_x, int center_y) {
	    super();
	    this.center_x = center_x;
	    this.center_y = center_y;
	}

	private int calcQuadrant(int x, int y) {
	    if (y<=center_y) {
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
	    
	    if (cmp != 0) return cmp;
	    
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
	    this.clockwise = new CircleClockwise_SORT(center_x,center_y);
	}

	@Override
	public int compare(Position o1, Position o2) {
	    return clockwise.compare(o1, o2) * -1;
	}
	
	
    }

    
    
}
