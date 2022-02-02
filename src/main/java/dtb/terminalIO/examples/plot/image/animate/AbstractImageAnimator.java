package dtb.terminalIO.examples.plot.image.animate;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import dtb.terminalIO.components.plot.image.animate.ImageIterator;
import dtb.terminalIO.geom.Dimension;

public abstract class AbstractImageAnimator implements ImageIterator, Dimension {

    protected BufferedImage buffer;

    protected Dimension oldSize;
    protected Dimension size;

    public AbstractImageAnimator(Dimension size) {
        setSize(size);
        buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    public AbstractImageAnimator() {
        this(new Dimension.Simple(300, 100));
    }
    
    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public BufferedImage next() {
        if (hasNext()) {
            paint(buffer.getGraphics());
        }
        return buffer;
    }

    Dimension getSize() {
        return size;
    }

    void setSize(Dimension size) {
        this.size = size;
    }

    public int getWidth() {
        return getSize().getWidth();
    }

    public int getHeight() {
        return getSize().getHeight();
    }

    abstract protected void paint(Graphics g);
}