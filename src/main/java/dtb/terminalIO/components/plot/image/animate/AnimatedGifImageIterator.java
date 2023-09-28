package dtb.terminalIO.components.plot.image.animate;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class AnimatedGifImageIterator implements ImageIterator {

    List<BufferedImage> images;
    
    int currentImageIDX = 0;
    boolean looping = true;
    
    public AnimatedGifImageIterator(List<BufferedImage> images) {
        this.images = images;
    }

    public AnimatedGifImageIterator(String gifPath) throws IOException {
        this(new GifReader().gifFileToImages(gifPath));
    }
    
    public int currentIDX() {
        return currentImageIDX;
    }
    
    
    @Override
    public boolean hasNext() {
        return (looping || currentImageIDX < images.size()-1);
    }

    @Override
    public BufferedImage next() {
        if (currentImageIDX >= images.size()) {
            currentImageIDX = 0;
        }

        BufferedImage image = images.get(currentImageIDX);
        currentImageIDX++;
        return image;
    }

    
    public static class GifReader {
        
        public List<BufferedImage> gifFileToImages(String gifPath) throws IOException {
            
            // A list to store the image buffers
            List<BufferedImage> images = new ArrayList<>();
            
            ImageInputStream input = null;
            
            try {
                // Create an image reader for GIF format
                ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
                
                // Create an image input stream from the file
                input = ImageIO.createImageInputStream(new File(gifPath));
                
                // Set the input source for the reader
                reader.setInput(input);
                
                // Get the number of frames in the GIF
                int numFrames = reader.getNumImages(true);
                
                // Loop through the frames and read them as buffered images
                for (int i = 0; i < numFrames; i++) {
                    BufferedImage image = reader.read(i);
                    images.add(image);
                }
                
            } finally {
                // Close the input stream
                input.close();
            }
            
            
            
            return images;
            
            
            
        }
        
    }    
    
    
}
