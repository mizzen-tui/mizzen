package dtb.terminalIO.components.plot.image.animate;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import dtb.terminalIO.components.plot.image.ImageRectangleAdapter;

public interface ImageIterator
        extends Iterator<BufferedImage>, AutoCloseable {

    /*
     * Narrow AutoCloseable.close().
     *
     * Implementations deal with any checked cleanup exceptions internally,
     * so users of ImageIterator do not have to catch Exception merely to
     * close an image sequence.
     */
    @Override
    void close();


    /**
     * An image iterator that merges the previously rendered image with
     * the current image.
     *
     * NOTE:
     *
     * AnimatedGifImageIterator no longer needs this wrapper. GIF frame
     * composition is handled internally by AnimatedGifImageIterator,
     * where frame offsets and disposal methods are available.
     */
    public static class CombinedImageIterator implements ImageIterator {

        private final ImageIterator originalIterator;
        private BufferedImage previousImage;

        public static CombinedImageIterator wrapWithCombinedImageIterator(
                ImageIterator originalIterator) {

            if (originalIterator instanceof CombinedImageIterator) {
                return (CombinedImageIterator) originalIterator;
            }

            return new CombinedImageIterator(originalIterator);
        }

        public CombinedImageIterator(ImageIterator originalIterator) {
            this.originalIterator = originalIterator;
        }

        @Override
        public boolean hasNext() {
            return originalIterator.hasNext();
        }

        @Override
        public BufferedImage next() {

            BufferedImage currentImage = originalIterator.next();

            if (previousImage != null) {

                ImageRectangleAdapter previousIMGSize =
                    new ImageRectangleAdapter(previousImage);

                BufferedImage combinedImage =
                    new BufferedImage(
                        previousIMGSize.getWidth(),
                        previousIMGSize.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);

                Graphics g = combinedImage.getGraphics();

                try {
                    g.drawImage(previousImage, 0, 0, null);
                    g.drawImage(currentImage, 0, 0, null);
                } finally {
                    g.dispose();
                }

                previousImage = combinedImage;

                return combinedImage;

            } else {

                previousImage = currentImage;

                return currentImage;
            }
        }

        @Override
        public void close() {
            previousImage = null;
            originalIterator.close();
        }
    }
}