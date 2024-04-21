package dtb.terminalIO.components.plot.image.animate;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.events.TerminalEvent;

public class ImageAnimatorComponent extends AbstractComponentWrappper<Component<?>> {

    ImageRenderRequest imageRenderRequest;
    ImageIterator animation;
    OnIdleEventEventHandler idleEventHandler;
    boolean stopped = false;

    public ImageAnimatorComponent(Color color,  ImageIterator animation, Dimension size) {
        super(color, null);
        this.animation = animation;
        this.imageRenderRequest = new ImageRenderRequest();
        try {
            imageRenderRequest.setDesiredImgSizeInCharacters(size);
            this.setWrappedComponent(ImageViewerFactory.fromImage(imageRenderRequest.setImageToView(animation.next())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setFillOnClear(false); 
    }
    
    public ImageAnimatorComponent(Color color, ImageRenderRequest imageRenderRequest, ImageIterator animation, Dimension size) {
        super(color, null);
        this.animation = animation;
        this.imageRenderRequest = imageRenderRequest;
        try {
            imageRenderRequest.setDesiredImgSizeInCharacters(size);
            this.setWrappedComponent(ImageViewerFactory.fromImage(imageRenderRequest.setImageToView(animation.next())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public ImageAnimatorComponent(Color color, 
            ImageIterator animation, 
            Dimension size, 
            EventBus eventBus) {
        
        this(color, animation, size, eventBus, 100);
    }
    
    public void stop() {
        this.stopped = true;
        this.idleEventHandler = null;
    }
    
    @Override
    public void setParentContainer(ParentContainer parentContainer) {
        super.setParentContainer(parentContainer);
        if (parentContainer == null) {
            this.stop();
        }
    }

    @Override
    public void resize(Dimension d) {

        if (this.isSameDimension(d)) {
            return ;
        }

        if (this.animation != null && this.animation instanceof CanResize) {
            Dimension imgSize = new Dimension.Simple(d);
            if (imageRenderRequest.getImageRenderType() == IMAGE_RENDER_TYPE.BLOCK) {
                imgSize = new Dimension.Simple(d.getHeight()*2, d.getWidth());
            } else if (imageRenderRequest.getImageRenderType() == IMAGE_RENDER_TYPE.BRAILLE) {
                imgSize = new Dimension.Simple(d.getHeight()*4, d.getWidth()*2);
            }
            ((CanResize)this.animation).resize(imgSize);
        }
        imageRenderRequest.setDesiredImgSizeInCharacters(d);

        super.resize(d);

    }


    public ImageAnimatorComponent(Color color, 
                                  ImageIterator animation, 
                                  Dimension size, 
                                  EventBus eventBus,
                                  int interval) {
        
        this(color, animation, size);

        if (ImageAnimatorComponent.this.animation.hasNext()) {
            setupTimer(eventBus, interval);
        }
    }

    public boolean canShowNext() {
        return ImageAnimatorComponent.this.animation.hasNext() && !stopped;
    }

    public boolean showNext() throws Exception {
        if (!canShowNext()) {
            return false;
        }
        ImageViewerFactory.fromImage(getWrappedComponent(), imageRenderRequest.setImageToView(ImageAnimatorComponent.this.animation.next()));
        this.markAsDirty();
        return true;
    }

    public boolean showNextAndDisplay(TerminalEvent<?> te) throws Exception {
        boolean result = this.showNext();
        if (result) {
            te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
        }
        return result;
    }

    public OnIdleEventEventHandler buildTimer(int interval) {
        if (this.idleEventHandler != null) {
            stopped = false;
            return this.idleEventHandler;
        }
        this.idleEventHandler = new OnIdleEventEventHandler.RepeatingIntervalWhileCond(interval,
                ()->canShowNext(),
                (te)-> {
                    try {
                        showNext();
                        te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }) ;
        stopped = false;
        return this.idleEventHandler;
    }

    public EventHandler setupTimer(EventBus eventBus, int interval) {
        this.idleEventHandler = buildTimer(interval);
        eventBus.appendIdleEventHandler(this.idleEventHandler);
        return this.idleEventHandler;
    }

    public OnIdleEventEventHandler getIdleEventEventHandler() {
        return this.idleEventHandler;
    }

    public ImageRenderRequest getImageRenderRequest() {
        return this.imageRenderRequest;
    }
}