package dtb.terminalIO.components.plot.image;

import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderer;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBPlotPredicate;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBToAnsiColorControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TimedRequestedController;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.Logging;

public class ResizableImageViewer extends AbstractComponentWrappper<ImageRenderer> {

    BufferedImageProvider imageToView;

    ImageRenderRequest imageRequest;
    ImageRenderRequest lastImageRequestRendered;

    TimedRequestedController timedOnResizeHandler;
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

        //getWrappedComponent().clear();

        buildTimedOnResizeHandler(eventBus).onRequest(eventBus);

    }

    private TimedRequestedController buildTimedOnResizeHandler(EventBus eventBus) {
        if (timedOnResizeHandler != null) {
            return timedOnResizeHandler;
        }

        timedOnResizeHandler = new TimedRequestedController(() -> {
            if (lastRequestSize == null) {
                return;
            }
            //resize(lastRequestSize);
            resizeOnSeparateThread(eventBus, lastRequestSize);
        });

        return timedOnResizeHandler;
    }

    private BlockingQueue<Runnable> taskQueue = null;
    private AtomicBoolean isProcessing = null;
    
    
    private <T> T buildAndGetSynchObjects(Supplier<T> theDesiredObject) {
        if (taskQueue == null) {
            taskQueue = new LinkedBlockingQueue<>();
            isProcessing = new AtomicBoolean(false);
        }
        return theDesiredObject.get();
    }
    
    private BlockingQueue<Runnable>  getTaskQueue() {
        return buildAndGetSynchObjects(()-> taskQueue);
    }
    
    private AtomicBoolean  isProcessing() {
        return buildAndGetSynchObjects(()-> isProcessing);
    }
    private void resizeOnSeparateThread(EventBus eventBus, Dimension size) {

        getTaskQueue().offer(() -> {
            ImageRenderer imgRenderer = getWrappedComponent();
            
            ImageRenderRequest newImageRequest = new ImageRenderRequest().copyAllFrom(imageRequest);
            newImageRequest.setDesiredImgSizeInCharacters(size);
            newImageRequest.setImageToView(imageToView.get());
            
            if (newImageRequest != null && newImageRequest.isSameRequest(lastImageRequestRendered)) {
                return;
            }

            try {
                this.imageRequest.setImageToView(imageToView.get());
                this.imageRequest.setDesiredImgSizeInCharacters(size);
                lastImageRequestRendered = new ImageRenderRequest().copyAllFrom(imageRequest);
                synchronized (drawMutex) {
                  getWrappedComponent().clear();
                  TextComponent resizingMessage = new TextComponent(
                                     "\n\n\n\n     Resizing to .. " + size.getHeight() + " X " + size.getWidth() + " \n\n");

                    this.setWrappedComponent( new ImageRenderer(resizingMessage) );
                }
                ImageViewerFactory.fromImage(imgRenderer, this.imageRequest);

                imgRenderer.resize(size);
                imgRenderer.getSegmentsBuffer(); // forces a draw in separate thread
                
            } catch (Exception e) {
                eventBus.raiseEvent(new TerminalEvent<Exception>(this, TerminalEvent.TYPE.EXCEPTION, e));
                return;
            }


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
                Logging.info("ResizableImageViewer  - RequestUpdateCanvasEvent!!!!!!!!!!!! " + imgRenderer.getClass());
                ResizableImageViewer.this.markAsDirty();
                getWrappedComponent().markAsDirty();
                eventBus.raiseEvent(new RequestUpdateCanvasEvent(ResizableImageViewer.this));
            });
        });

        eventBus.asynch((te)->{
            processNextTask(eventBus);
        });
        
    }

    private void processNextTask(EventBus eventBus) {
        if (isProcessing().compareAndSet(false, true)) {
            new Thread(() -> {
                try {
                    Runnable task = getTaskQueue().take();
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    isProcessing().set(false);
                    if (!getTaskQueue().isEmpty()) {
    //  enqueue ob event bus
                        eventBus.asynch((te)->{
                            processNextTask(eventBus);
                        });
                    }
                }
            }).start();
        }
    }

}