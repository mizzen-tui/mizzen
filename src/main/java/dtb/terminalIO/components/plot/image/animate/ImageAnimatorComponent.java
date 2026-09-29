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
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.events.TerminalEvent;

public class ImageAnimatorComponent
        extends AbstractComponentWrappper<Component<?>>
        implements AutoCloseable {

    ImageRenderRequest imageRenderRequest;
    ImageIterator animation;

    OnIdleEventEventHandler idleEventHandler;

    /*
     * Remember where idleEventHandler was registered so that stop()
     * can genuinely unregister it.
     */
    EventHandlerContainer eventHandlerContainer;

    boolean stopped = false;
    boolean closed = false;


    public ImageAnimatorComponent(
            Color color,
            ImageIterator animation,
            Dimension size) {

        super(color, null);

        this.animation = animation;
        this.imageRenderRequest = new ImageRenderRequest();

        try {

            imageRenderRequest.setDesiredImgSizeInCharacters(size);

            this.setWrappedComponent(
                ImageViewerFactory.fromImage(
                    imageRenderRequest.setImageToView(
                        animation.next())));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        setFillOnClear(false);
    }


    public ImageAnimatorComponent(
            Color color,
            ImageRenderRequest imageRenderRequest,
            ImageIterator animation,
            Dimension size) {

        super(color, null);

        this.animation = animation;
        this.imageRenderRequest = imageRenderRequest;

        try {

            imageRenderRequest.setDesiredImgSizeInCharacters(size);

            this.setWrappedComponent(
                ImageViewerFactory.fromImage(
                    imageRenderRequest.setImageToView(
                        animation.next())));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public ImageAnimatorComponent(
            Color color,
            ImageIterator animation,
            Dimension size,
            EventBus eventBus) {

        this(color, animation, size, eventBus, 100);
    }


    /*
     * STOP means:
     *
     *     stop generating animation events
     *
     * It deliberately does NOT mean:
     *
     *     destroy the animation
     *
     * This makes stop reversible.
     */
    public void stop() {

        stopped = true;

        if (eventHandlerContainer != null &&
                idleEventHandler != null) {

            eventHandlerContainer.removeEventHandler(
                idleEventHandler);
        }

        idleEventHandler = null;
        eventHandlerContainer = null;
    }


    /*
     * Permanent lifecycle end.
     */
    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed = true;

        stop();

        if (animation != null) {
            animation.close();
            animation = null;
        }
    }


    /*
     * Replace the animation owned by this component.
     *
     * The old animation is no longer required, so this is an
     * unambiguous place to close it.
     *
     * Passing null is also valid and releases the old animation.
     */
    public void setAnimation(ImageIterator newAnimation) {

        if (animation == newAnimation) {
            return;
        }

        stop();

        if (animation != null) {
            animation.close();
        }

        animation = newAnimation;
        closed = false;
    }


    @Override
    public void setParentContainer(
            ParentContainer parentContainer) {

        super.setParentContainer(parentContainer);

        /*
         * Detachment is NOT destruction.
         *
         * Stop the timer, but leave the ImageIterator usable.
         */
        if (parentContainer == null) {
            stop();
        }
    }


    @Override
    public void resize(Dimension d) {

        if (this.isSameDimension(d)) {
            return;
        }

        if (this.animation != null &&
                this.animation instanceof CanResize) {

            Dimension imgSize =
                new Dimension.Simple(d);

            if (imageRenderRequest.getImageRenderType() ==
                    IMAGE_RENDER_TYPE.BLOCK) {

                imgSize =
                    new Dimension.Simple(
                        d.getHeight() * 2,
                        d.getWidth());

            } else if (
                imageRenderRequest.getImageRenderType() ==
                    IMAGE_RENDER_TYPE.BRAILLE) {

                imgSize =
                    new Dimension.Simple(
                        d.getHeight() * 4,
                        d.getWidth() * 2);
            }

            ((CanResize) this.animation).resize(imgSize);
        }

        imageRenderRequest
            .setDesiredImgSizeInCharacters(d);

        super.resize(d);
    }


    public ImageAnimatorComponent(
            Color color,
            ImageIterator animation,
            Dimension size,
            EventBus eventBus,
            int interval) {

        this(color, animation, size);

        if (this.animation.hasNext()) {
            setupTimer(eventBus, interval);
        }
    }


    public boolean canShowNext() {

        return !closed
            && !stopped
            && animation != null
            && animation.hasNext();
    }


    public boolean showNext() throws Exception {

        if (!canShowNext()) {
            return false;
        }

        ImageViewerFactory.fromImage(
            getWrappedComponent(),
            imageRenderRequest.setImageToView(
                animation.next()));

        this.markAsDirty();

        return true;
    }


    public boolean showNextAndDisplay(
            TerminalEvent<?> te)
            throws Exception {

        boolean result = this.showNext();

        if (result) {

            te.getEventBus().raiseEvent(
                new RequestUpdateCanvasEvent(this));
        }

        return result;
    }


    public OnIdleEventEventHandler buildTimer(
            int interval) {

        if (this.idleEventHandler != null) {

            stopped = false;

            return this.idleEventHandler;
        }

        this.idleEventHandler =
            new OnIdleEventEventHandler
                .RepeatingIntervalWhileCond(
                    interval,
                    () -> canShowNext(),
                    (te) -> {

                        try {

                            showNext();

                            te.getEventBus().raiseEvent(
                                new RequestUpdateCanvasEvent(
                                    this));

                        } catch (Exception e) {

                            throw new RuntimeException(e);
                        }
                    });

        stopped = false;

        return this.idleEventHandler;
    }


    public EventHandler setupTimer(
            EventBus eventBus,
            int interval) {

        /*
         * Prevent accidental double registration.
         */
        stop();

        this.eventHandlerContainer = eventBus;

        this.idleEventHandler =
            buildTimer(interval);

        this.eventHandlerContainer
            .appendEventHandler(
                this.idleEventHandler);

        return this.idleEventHandler;
    }


    public OnIdleEventEventHandler
            getIdleEventEventHandler() {

        return this.idleEventHandler;
    }


    public ImageRenderRequest
            getImageRenderRequest() {

        return this.imageRenderRequest;
    }
}