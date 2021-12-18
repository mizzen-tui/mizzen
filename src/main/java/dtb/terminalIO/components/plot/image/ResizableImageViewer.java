package dtb.terminalIO.components.plot.image;

import java.awt.image.BufferedImage;

import dtb.terminalIO.components.AbstractComponentWithContainer;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBPlotPredicate;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBToAnsiColorControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.AbstractTimedRequestedController;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;

public class ResizableImageViewer extends AbstractComponentWithContainer<Composite> {

    BufferedImageProvider imageToView;
    Component<?> imgViewer;

    ImageRenderRequest imageRequest;
    AbstractTimedRequestedController timedOnResizeHandler;
    Dimension lastRequestSize;
    EventHandler onImageLoaded;

    public ResizableImageViewer(Color color, Dimension d, BufferedImageProvider imageToView) {
        super(color, new Composite(color));
        getWrappedComponent().setBounds(d);
        this.imageToView = imageToView;
        this.imageRequest = new ImageRenderRequest();
        this.imageRequest.setDesiredImgSizeInCharacters(d);
        this.setPlotPredicate(ImageViewerFactory.PLOT_DARK_ONLY);
        this.setColorMapping(ImageViewerFactory.rgbColorMapRCol);
        this.setImageRenderType(IMAGE_RENDER_TYPE.BRAILLE);
    }
    
    public ResizableImageViewer(Color color, Dimension d, BufferedImage imageToView) {
        this(color, d, new BufferedImageProvider.Simple(imageToView));
    }


    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
        
        if (this.getParentContainer() != null) {
            this.getParentContainer().childResized(this);
        }
    }
    
    public ResizableImageViewer setOnImageLoaded(EventHandler onImageLoaded) {
        this.onImageLoaded = onImageLoaded;
        return this;
    }

    public ResizableImageViewer setPlotPredicate(RGBPlotPredicate plotPredicate) {
        this.imageRequest.setToPlotOrNot(plotPredicate);
        return this;
    }

    public ResizableImageViewer setColorMapping(RGBToAnsiColorControlCode colorMapping) {
        this.imageRequest.setColorMap(colorMapping);
        return this;
    }

    public ResizableImageViewer setImageRenderType(IMAGE_RENDER_TYPE imageRenderType) {
        this.imageRequest.setImageRenderType(imageRenderType);
        return this;
    }

    private AbstractTimedRequestedController buildTimedOnResizeHandler(EventBus eventBus) {
        if (timedOnResizeHandler != null) {
            return timedOnResizeHandler;
        }

        timedOnResizeHandler = new AbstractTimedRequestedController(() -> {
            if (lastRequestSize == null) {
                return;
            }
            resizeOnSeparateThread(eventBus, lastRequestSize);
        }) {
            @Override
            protected boolean isRequestDifferentFromPrevious() {
                return true;
            }
        };

        return timedOnResizeHandler;
    }

    public synchronized void resizeImageAsynch(EventBus eventBus, Dimension size) {

        ResizableImageViewer.this.lastRequestSize = size;

        getWrappedComponent().clearAndRemoveAllChildren();

        TextComponent resizingMessage = new TextComponent(
                "\n\n   Resizing to .. " + size.getHeight() + " X " + size.getWidth() + " \n\n");

        getWrappedComponent().add(2, 2, resizingMessage);

        buildTimedOnResizeHandler(eventBus).onRequest(eventBus);

    }

    private void resizeOnSeparateThread(EventBus eventBus, Dimension size) {
        new Thread(() -> {

            try {
                
                this.imageRequest.setImageToView(imageToView.get());
                this.imageRequest.setDesiredImgSizeInCharacters(size);
                
                if (imgViewer == null) {
                    
                    imgViewer = ImageViewerFactory.fromImage(this.imageRequest);
                    
                } else {
                    ImageViewerFactory.fromImage(imgViewer, this.imageRequest);
                }
            } catch (Exception e) {
                eventBus.raiseEvent(new TerminalEvent<Exception>(this, TerminalEvent.TYPE.EXCEPTION, e));
                return;
            }

            ((CanResize) imgViewer).resize(size);

            // for concurrency / safety only perform the component update during an event cycle
            eventBus.asynch((te) -> {

                if ( ! getWrappedComponent().containsChild(imgViewer)) {
                    getWrappedComponent().clearAndRemoveAllChildren();
                    getWrappedComponent().add(imgViewer);
                }
                resize(imgViewer.getPreferredRectangleForLayout());
                if (onImageLoaded != null) {
                    onImageLoaded.handle(te);
                }
            });

        }).start();
    }


}