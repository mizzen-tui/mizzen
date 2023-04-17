package dtb.terminalIO.components.plot.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

public interface BufferedImageProvider extends Supplier<BufferedImage> {

    public static class Simple implements BufferedImageProvider {

        BufferedImage image;

        public Simple(BufferedImage image) {
            this.image = image;
        }

        @Override
        public BufferedImage get() {
            return image;
        }

    }
    
    public static class Delegate implements BufferedImageProvider {
        BufferedImageProvider delegate;

        @Override
        public BufferedImage get() {
            return delegate.get();
        } 


    }

    static class ImageCache {
        
        static Map<String, BufferedImage> namedImages = new HashMap<String, BufferedImage>();
        
        static void put(String name, BufferedImage image) {
            namedImages.put(name, image);
        }
        
        static BufferedImage fromResource(String resourceName) {
            BufferedImage image = namedImages.get(resourceName);
            if (image != null) {
                return image;
            }
            try {
                image = ImageIO.read(ImageCache.class.getResourceAsStream(resourceName));
                put(resourceName, image);
            } catch (IOException e) {
                throw new IllegalArgumentException("could not load image from named resource " + resourceName + ".\nDetails: "+ e.getMessage());
            }
            return image;
        }
        
        static BufferedImage fromFilePath(String filePath) {
            BufferedImage image = namedImages.get(filePath);
            if (image != null) {
                return image;
            }
            try {
                image = ImageIO.read(new FileInputStream(new File(filePath)));
                put(filePath, image);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not load image from path " + filePath + ".\nDetails: "+ e.getMessage());
            }
            return image;
        }
    }

    public static class FromResource implements BufferedImageProvider {

        String resourceName;
        BufferedImage image;
        
        public FromResource(String resourceName) {
            this.resourceName = resourceName;
        }
        
        @Override
        public BufferedImage get() {
            if (image != null) {
                return image;
            }
            image = ImageCache.fromResource(resourceName);
            return image;
        }
    }

    public static class FromFilePath implements BufferedImageProvider {

        String filePath;
        BufferedImage image;
        
        public FromFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        @Override
        public BufferedImage get() {
            if (image != null) {
                return image;
            }
            image = ImageCache.fromFilePath(filePath);
            return image;
        }
    }
    
    
}
