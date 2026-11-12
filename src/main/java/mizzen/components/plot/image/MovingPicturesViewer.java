package mizzen.components.plot.image;

import mizzen.components.ComponentWithFocus;
import mizzen.components.Component;
import mizzen.components.composite.Composite;
import mizzen.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import mizzen.components.plot.image.animate.ImageAnimatorComponent;
import mizzen.components.plot.image.animate.ImageIterator;
import mizzen.controlcodes.color.Color;
import mizzen.events.ChainedEventHandler;
import mizzen.events.EventBus;
import mizzen.events.TerminalEvent;
import mizzen.geom.Align;
import mizzen.geom.VAlign;
import mizzen.geom.position.Position;
import mizzen.geom.position.PositionRelativeFactory;

/**
 * An animation (an ImageIterator's frames) with an Options button top right that opens a dialog of
 * render settings. Border and title are its panel's.
 * <pre>
 *     new MovingPicturesViewer(new AnimatedGifImageIterator(bytes), rqst).withoutOptions().start(te, 100)
 * </pre>
 */
public class MovingPicturesViewer extends ComponentWithFocus<Composite, MovingPicturesViewer> implements AutoCloseable {

    ChainedEventHandler chainedEventHandler;
    ImageAnimatorComponent animatorComponent;
    Composite imageViewer;
    Component<?> optionsButton;

    /** as the constructor with a colour first, in its theme's colours */
    public MovingPicturesViewer(ImageIterator imageIterator, ImageRenderRequest rqst) {
        this((Color) null, imageIterator, rqst);
    }


    public MovingPicturesViewer(Color color, ImageIterator imageIterator, ImageRenderRequest rqst) {

        super(color, new Composite(color));

        imageViewer = getPanel();

        //imageViewer = new BorderedComponent(new TextComponent("Placeholder"));

        // the options dialog's "settings changed" events reach the dialog's handlers through here
        chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
        focus().handleEvents(chainedEventHandler.asConditionalEventHandler());

        optionsButton = ImageManipulationControls.popupOnClick("Options", rqst, imageViewer, chainedEventHandler, null);

        //animatorComponent = new ImageAnimatorComponent(color, rqst,  CombinedImageIterator.wrapWithCombinedImageIterator(imageIterator), rqst.getDesiredImgSizeInCharacters());
        animatorComponent = new ImageAnimatorComponent(color, rqst,  imageIterator, rqst.getDesiredImgSizeInCharacters());

        //imageViewer.setComponent(animatorComponent);

        imageViewer.add(animatorComponent);
        Position bPos = PositionRelativeFactory.fromAlign(VAlign.TOP, Align.RIGHT, imageViewer, optionsButton);
        imageViewer.add(bPos,  optionsButton);


    }
    
    /** the animation alone: no Options button (a viewer that only shows, or one whose settings are set in code) */
    public MovingPicturesViewer withoutOptions() {
        if (optionsButton != null) {
            imageViewer.remove(optionsButton);
            optionsButton = null;
        }
        return this;
    }

    public MovingPicturesViewer start(EventBus eventBus, int interval) {
        animatorComponent.setupTimer(eventBus, interval);
        return this;
    }
    
    public MovingPicturesViewer start(TerminalEvent<?> te, int interval) {
        return start(te.getEventBus(), interval);
    }

    @Override
    public void close() {
        animatorComponent.close();
    }    

}
