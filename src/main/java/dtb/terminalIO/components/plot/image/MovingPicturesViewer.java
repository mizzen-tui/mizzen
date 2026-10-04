package dtb.terminalIO.components.plot.image;

import dtb.terminalIO.components.AbstractWrappedComponentWithFocusManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasTitleAndBorder;
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

public class MovingPicturesViewer extends AbstractWrappedComponentWithFocusManager<Composite> 
      implements HasTitleAndBorder<MovingPicturesViewer>, AutoCloseable  {

    ChainedEventHandler chainedEventHandler;
    ImageAnimatorComponent animatorComponent;
    Composite imageViewer;

    public MovingPicturesViewer(Color color, ImageIterator imageIterator, ImageRenderRequest rqst) {

        super(color, new Composite(color));

        imageViewer = getWrappedComponent();

        //imageViewer = new BorderedComponent(new TextComponent("Placeholder"));

        chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
        setCustomExtendedEventHandler( chainedEventHandler.asConditionalEventHandler()  );

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
    public void hilite() {
        imageViewer.hilite();
    }

    @Override
    public void unHilite() {
        imageViewer.unHilite();
    }

    @Override
    public boolean hasBorder() {
        return imageViewer.hasBorder();
    }

    @Override
    public MovingPicturesViewer setBorder(boolean hasBorder) {
        imageViewer.setBorder(hasBorder);
        return this;
    }

    @Override
    public MovingPicturesViewer setBorderColor(Color borderColors) {
        imageViewer.setBorderColor(borderColors);
        return this;
    }

    @Override
    public MovingPicturesViewer setTitle(Color titleColors, String title) {
        imageViewer.setTitle(titleColors, title);
        return this;
    }

    @Override
    public MovingPicturesViewer setTitle(String title) {
        imageViewer.setTitle(title);
        return this;
    }

    @Override
    public String getTitle() {
        return imageViewer.getTitle();
    }
    
    @Override
    public void close() {
        animatorComponent.close();
    }    

}
