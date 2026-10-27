package mizzen.components.plot.image;

import java.awt.Image;
import java.awt.image.ImageObserver;

import mizzen.geom.Rectangle;

/**
 * Adapts an Image object to look like a Rectangle
 */
public class ImageRectangleAdapter implements Rectangle {

    private Image image;
    private ImageObserver observer;

    public ImageRectangleAdapter(Image image) {
        this.image = image;
        this.observer = new ImageHandler();
    }

    @Override
    public int getRow() {
        return 0;
    }

    @Override
    public int getCol() {
        return 0;
    }

    @Override
    public int getWidth() {
        return getImageWidth(image);
    }

    @Override
    public int getHeight() {
        return getImageHeight(image);
    }

    @Override
    public void moveTo(int row, int col) {
        throw new IllegalStateException("ImageRectangleAdapter.moveTo(): Cannot perform a move!");

    }
    
    public String toString() {
        return new Rectangle.Simple(this).toString();
    }

    private synchronized int getImageWidth(Image img) {

        /* Wait for the width of the image to
         * become available.
         */
        while (img.getWidth(observer) == -1) {
            try {
                wait(); // <-- this might turn into a performance bottleneck
            } catch (InterruptedException e) {
            }
        }


        return img.getWidth(observer);
    }

    private synchronized int getImageHeight(Image img) {

        /* Wait for the height of the image to
         * become available.
         */
        while (img.getHeight(observer) == -1) {
            try {
                wait(); // <-- this might turn into a performance bottleneck
            } catch (InterruptedException e) {
            }
        }

        return img.getHeight(observer);
    }

    private class ImageHandler implements ImageObserver {

        public synchronized boolean imageUpdate(Image img, int infoFlags, int x, int y, int width, int height) {

            boolean gotInfo = true;

            if ((infoFlags & (WIDTH | HEIGHT)) == (WIDTH | HEIGHT)) {
                gotInfo = false;
                notify();
            }

            return gotInfo;
        }

    }

}
