package mizzen.examples.plot.image.animate;

import java.awt.Color;
import java.awt.Graphics;

import mizzen.geom.Dimension;

public class BufferedAnimate extends AbstractImageAnimator {

    public BufferedAnimate() {
        super();
    }

    public BufferedAnimate(Dimension size) {
        super(size);
    }

    Color colors[] = { Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA };

    protected void paint(Graphics bufferG) {

        // Graphics g = buffer.getGraphics();
        // Calculate each time in case of resize
        int x = 0;
        int y = 0;
        int width = size.getWidth();
        int height = size.getHeight();
        int start = 0;
        int steps = colors.length;
        int stepSize = 360 / steps;
        synchronized (colors) {
            bufferG.setColor(Color.WHITE);
            bufferG.fillRect(x, y, width, height);
            for (int i = 0; i < steps; i++) {
                bufferG.setColor(colors[i]);
                bufferG.fillArc(x, y, width, height, start, stepSize);
                start += stepSize;
            }
            nextImageFrame();
        }
    }

    private void nextImageFrame() {
        Color c = colors[0];
        synchronized (colors) {
            System.arraycopy(colors, 1, colors, 0, colors.length - 1);
            colors[colors.length - 1] = c;
        }
    }


}