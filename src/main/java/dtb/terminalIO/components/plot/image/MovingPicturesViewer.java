package dtb.terminalIO.components.plot.image;

import dtb.terminalIO.components.ComponentWithFocus;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.components.plot.image.animate.ImageAnimatorComponent;
import dtb.terminalIO.components.plot.image.animate.ImageIterator;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;

/**
 * An animation (an ImageIterator's frames) with an Options button top right that opens a dialog of
 * render settings. Border and title are its panel's.
 */
public class MovingPicturesViewer extends ComponentWithFocus<Composite, MovingPicturesViewer> implements AutoCloseable {

    ChainedEventHandler chainedEventHandler;
    ImageAnimatorComponent animatorComponent;
    Composite imageViewer;

    public MovingPicturesViewer(Color color, ImageIterator imageIterator, ImageRenderRequest rqst) {

        super(color, new Composite(color));

        imageViewer = getPanel();

        //imageViewer = new BorderedComponent(new TextComponent("Placeholder"));

        // the options dialog's "settings changed" events reach the dialog's handlers through here
        chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
        focus().handleEvents(chainedEventHandler.asConditionalEventHandler());

        Component<?> popupImageControlsButton = ImageManipulationControls.popupOnClick("Options", rqst, imageViewer, chainedEventHandler, null);

        //animatorComponent = new ImageAnimatorComponent(color, rqst,  CombinedImageIterator.wrapWithCombinedImageIterator(imageIterator), rqst.getDesiredImgSizeInCharacters());
        animatorComponent = new ImageAnimatorComponent(color, rqst,  imageIterator, rqst.getDesiredImgSizeInCharacters());

        //imageViewer.setComponent(animatorComponent);

        imageViewer.add(animatorComponent);
        Position bPos = PositionRelativeFactory.fromAlign(VAlign.TOP, Align.RIGHT, imageViewer, popupImageControlsButton);
        imageViewer.add(bPos,  popupImageControlsButton);


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
