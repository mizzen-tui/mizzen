package dtb.terminalIO.components.plot.image;

import java.awt.image.BufferedImage;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderer;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBPlotPredicate;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBToAnsiColorControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.AbstractTimedRequestedController;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.Logging;

public class ResizableImageViewer extends AbstractComponentWrappper<ImageRenderer> {

    BufferedImageProvider imageToView;

    ImageRenderRequest imageRequest;
    ImageRenderRequest lastImageRequestRendered;
    
    AbstractTimedRequestedController timedOnResizeHandler;
    Dimension lastRequestSize;
    EventHandler onImageLoaded;
    Object drawMutex = new Object();

    public ResizableImageViewer(Color color, Dimension d, BufferedImageProvider imageToView) {
        super(color, ImageViewerFactory.emptyImage(color));
        getWrappedComponent().resize(d);
        this.imageToView = imageToView;
        this.imageRequest = new ImageRenderRequest();
        this.imageRequest.setDesiredImgSizeInCharacters(d);
        this.setPlotPredicate(ImageViewerFactory.PLOT_DARK_ONLY);
        this.setColorMapping(ImageViewerFactory.rgbColorMapRCol);
        this.setImageRenderType(IMAGE_RENDER_TYPE.BRAILLE);
        this.setFillOnClear(false);
    }

    public ResizableImageViewer(Color color, Dimension d, BufferedImage imageToView) {
        this(color, d, new BufferedImageProvider.Simple(imageToView));
    }

    @Override
    public void resize(Dimension d) {
        
        if (this.isSameDimension(d)) {
            return ;
        }
        
        lastImageRequestRendered = null; // forces img resize
        super.resize(d);
        
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

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        Logging.info("ResizableImage draw called!" + String.valueOf(this));

        synchronized ( drawMutex ) {
            super.draw(color, segments);
        }
    }

    public synchronized void resizeImageAsynch(EventBus eventBus, Dimension size) {

        this.lastRequestSize = size;

        getWrappedComponent().clear();

//        TextComponent resizingMessage = new TextComponent(
//                "\n\n\n\n     Resizing to .. " + size.getHeight() + " X " + size.getWidth() + " \n\n");
//
//        getWrappedComponent().setWrappedComponent(resizingMessage);

        buildTimedOnResizeHandler(eventBus).onRequest(eventBus);

    }

    private AbstractTimedRequestedController buildTimedOnResizeHandler(EventBus eventBus) {
        if (timedOnResizeHandler != null) {
            return timedOnResizeHandler;
        }

        timedOnResizeHandler = new AbstractTimedRequestedController(() -> {
            if (lastRequestSize == null) {
                return;
            }
            resize(lastRequestSize);
            resizeOnSeparateThread(eventBus, lastRequestSize);
        }) {
            @Override
            protected boolean isRequestDifferentFromPrevious() {
                return true; // lastRequestSize != null && !lastRequestSize.isSameDimension(imageRequest.desiredImgSizeInCharacters);
            }
        };

        return timedOnResizeHandler;
    }

    private void resizeOnSeparateThread(EventBus eventBus, Dimension size) {

        ImageRenderer imgRenderer = getWrappedComponent();
        
        ImageRenderRequest newImageRequest = new ImageRenderRequest().copyAllFrom(imageRequest);
        newImageRequest.setDesiredImgSizeInCharacters(size);
        newImageRequest.setImageToView(imageToView.get());

        if (newImageRequest != null && newImageRequest.isSameRequest(lastImageRequestRendered)) {
            return;
        }

        new Thread(() -> {

            try {
                this.imageRequest.setImageToView(imageToView.get());
                this.imageRequest.setDesiredImgSizeInCharacters(size);
                lastImageRequestRendered = new ImageRenderRequest().copyAllFrom(imageRequest);
                synchronized (drawMutex) {
                  TextComponent resizingMessage = new TextComponent(
                                     "\n\n\n\n     Resizing to .. " + size.getHeight() + " X " + size.getWidth() + " \n\n");

                    this.setWrappedComponent( new ImageRenderer(resizingMessage) );
                }
                ImageViewerFactory.fromImage(imgRenderer, this.imageRequest);

            } catch (Exception e) {
                eventBus.raiseEvent(new TerminalEvent<Exception>(this, TerminalEvent.TYPE.EXCEPTION, e));
                return;
            } 

            imgRenderer.resize(size);
            imgRenderer.getSegmentsBuffer(); // forces a draw in separate thread

            // for concurrency / safety only perform the component update during an event cycle
            eventBus.asynch((te) -> {

                synchronized (drawMutex) {
                    this.setWrappedComponent( imgRenderer );
                }

                this.childResized(imgRenderer);
                
                resize(getWrappedComponent().getPreferredRectangleForLayout());
                if (onImageLoaded != null) {
                    onImageLoaded.handle(te);
                }
                ResizableImageViewer.this.markAsDirty();
                eventBus.raiseEvent(new RequestUpdateCanvasEvent(ResizableImageViewer.this));
            });

        }).start();
    }


}