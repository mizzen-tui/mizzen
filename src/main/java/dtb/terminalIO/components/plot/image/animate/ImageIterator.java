package dtb.terminalIO.components.plot.image.animate;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import dtb.terminalIO.components.plot.image.ImageRectangleAdapter;

public interface ImageIterator extends Iterator<BufferedImage> {

    /***
     * An image iterator that merges the previously rendered image with the current image.
     * 
     * <p/>
     * 
     * Why do this?
     * 
     * <p/>
     * 
     * Useful when iterating over the images embedded in a GIF.  Quite often the images in a GIF do not
     * have the full image on the 2nd 3rd and nth image - but rather only store the pixels that have changed
     * This feature, though nice for memory preservation does not help with rendering.  Hence the need to merge images
     * to get the full pixels on every image iteration
     * 
     * @author dbalme
     *
     */
    static public class  CombinedImageIterator implements ImageIterator {
        private final Iterator<BufferedImage> originalIterator;
        private BufferedImage previousImage;


        public static CombinedImageIterator wrapWithCombinedImageIterator(ImageIterator originalIterator) {
            if (originalIterator instanceof CombinedImageIterator) {
                return (CombinedImageIterator) originalIterator;
            }
            return new CombinedImageIterator(originalIterator);
        }

        public CombinedImageIterator(Iterator<BufferedImage> originalIterator) {
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
                // Create a new BufferedImage that combines the previous and current images
                
                ImageRectangleAdapter previouseIMGSize = new ImageRectangleAdapter(previousImage);
                
                BufferedImage combinedImage = new BufferedImage(previouseIMGSize.getWidth(), previouseIMGSize.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics g = combinedImage.getGraphics();

                // Draw the previous image
                g.drawImage(previousImage, 0, 0, null);

                // Draw the current image on top of it
                g.drawImage(currentImage, 0, 0, null);
                // The combined image becomes the "previous" image for the next iteration
                previousImage = combinedImage;
                return combinedImage;
            } else {
                // The first image becomes the "previous" image for the next iteration
                previousImage = currentImage;

                return currentImage;
            }
        }
        
    }


}