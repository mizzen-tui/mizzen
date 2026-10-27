package mizzen.components.plot.image.animate;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mizzen.geom.position.Position;
import mizzen.geom.position.Vec2;

public class ImageSplitter implements ImageIterator {

    Image fullImage;
    List<Vec2<?>> offsets;

    // please note: This is the height/width of image segment in pixels 
    //              It is not meant to represent size in chars
    int height_subImage;
    int width_subImage;

    int currentImageIDX = 0;
    boolean looping = true;

    public ImageSplitter(Image fullImage, Collection<? extends Vec2<?>> offsets, int height_subImage, int width_subImage) {
        super();
        this.fullImage = fullImage;
        this.offsets = new ArrayList<Vec2<?>>(offsets);
        this.height_subImage = height_subImage;
        this.width_subImage = width_subImage;
    }

    public ImageSplitter(Image fullImage, int[] xpos, int[] ypos, int height_subImage, int width_subImage) {
        super();
        this.fullImage = fullImage;
        this.height_subImage = height_subImage;
        this.width_subImage = width_subImage;
        offsets = new ArrayList<Vec2<?>>(xpos.length * ypos.length);
        for (int j=0; j<ypos.length; j++) {
            for (int i=0; i<xpos.length; i++ ) {
                offsets.add(new Position.Simple(ypos[j], xpos[i]));
            }
        }
    }

    public int currentIDX() {
        return currentImageIDX;
    }

    public Vec2<?> getcurrentOffset() {
        int idx = 0; 
        if (currentImageIDX == 0) {
            idx = offsets.size()-1;
        } else {
            idx = currentImageIDX - 1;
        }
        return offsets.get(idx);
    }

    @Override
    public boolean hasNext() {
        return (looping || currentImageIDX < offsets.size()-1);
    }

    @Override
    public BufferedImage next() {
        if (currentImageIDX >= offsets.size()) {
            currentImageIDX = 0;
        }

        Vec2<?> offset = offsets.get(currentImageIDX);
        currentImageIDX++;
        BufferedImage subImage = ((BufferedImage) fullImage).getSubimage((int)offset.getX(), (int)offset.getY(), width_subImage, height_subImage);
        return subImage;
    }

    @Override
    public void close() {
       // NOOP
    }
}
