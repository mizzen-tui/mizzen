package dtb.terminalIO.components.plot.image;

import dtb.terminalIO.components.AbstractComponentWithContainer;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;

public class ImageManipulationControls extends AbstractComponentWithContainer<Composite> implements HasValue<ImageRenderRequest> {

    private ImageRenderRequest rqst;

    public ImageManipulationControls(Color color, ImageRenderRequest rqst) {
        super(color, new VerticalPanel().setVerticalSpacer(2));

        this.rqst = rqst;

        Composite controls = getWrappedComponent();

        DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE> imageRendererTypeDDLB = new DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE>(ImageViewerFactory.imageRenderTypeOptions());
        imageRendererTypeDDLB.setName("imageRendererTypeDDLB");
        imageRendererTypeDDLB.setValue( ImageManipulationControls.this.rqst.getImageRenderType() );
        imageRendererTypeDDLB.setColor(color);

        imageRendererTypeDDLB.onChange((te) -> {
            rqst.setImageRenderType(imageRendererTypeDDLB.getValue());
            te.getEventBus().raiseEvent( new HasValue.ValueChangedEvent<>(ImageManipulationControls.this) );
            te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(ImageManipulationControls.this));
        });

        controls.add(imageRendererTypeDDLB);

        DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode> colorMapDDLB = new DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode>(ImageViewerFactory.colorMap());
        colorMapDDLB.setName("colorMapDDLB");
        colorMapDDLB.setValue( ImageManipulationControls.this.rqst.getColorMap() );
        colorMapDDLB.onChange((te)->{
            rqst.setColorMap( colorMapDDLB.getValue()  );
            te.getEventBus().raiseEvent( new HasValue.ValueChangedEvent<>(ImageManipulationControls.this) );
            te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(ImageManipulationControls.this));
        });

        colorMapDDLB.setColor(color);

        controls.add(colorMapDDLB);

        DropdownListBox<ImageViewerFactory.RGBPlotPredicate> plotPredicatesDDLB = new DropdownListBox<ImageViewerFactory.RGBPlotPredicate>(ImageViewerFactory.plotPointPredicateOptions());

        plotPredicatesDDLB.setName("plotPredicatesDDLB");

        plotPredicatesDDLB.setValue( ImageManipulationControls.this.rqst.getToPlotOrNot()!=null
                                     ? ImageManipulationControls.this.rqst.getToPlotOrNot() 
                                     : ImageViewerFactory.plotPointPredicateOptions()[0].getValue() );
        plotPredicatesDDLB.onChange((te) -> {
            rqst.setToPlotOrNot( plotPredicatesDDLB.getValue() );
            te.getEventBus().raiseEvent( new HasValue.ValueChangedEvent<>(ImageManipulationControls.this) );
            te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(ImageManipulationControls.this));
        });
        plotPredicatesDDLB.setColor(color);

        controls.add(plotPredicatesDDLB);

    }

    public ImageManipulationControls(ImageRenderRequest rqst) {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED, rqst);
    }

    @Override
    public ImageRenderRequest getValue() {
        return this.rqst;
    }

    OnValueChangedEventHandler<ImageRenderRequest> actionOnChangeHandler;

    public OnValueChangedEventHandler<ImageRenderRequest> onChange(EventHandler handler, EventHandlerContainer eventHandlerContainer) {

        OnValueChangedEventHandler<ImageRenderRequest> onChangedHandler = actionOnChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, actionOnChangeHandler, eventHandlerContainer, handler);

        return onChangedHandler;
        
    }

    public static Button popupOnClick(ImageRenderRequest rqst, ChildComponentManager<?> parentPanel, EventHandlerContainer mainFocusManager, EventHandler actionOnChangeHandler) {
        return popupOnClick("+", rqst,  parentPanel, mainFocusManager, actionOnChangeHandler); 
    }

    public static Button popupOnClick(String buttonText, ImageRenderRequest rqst, ChildComponentManager<?> parentPanel, EventHandlerContainer eventHandlerContainer,  EventHandler actionOnChangeHandler) {
        Button popControlsButton = new Button(buttonText);
        popControlsButton.onChange((te)->{
            // TODO - need to handle multiple calls and suppress - while existing active 
            showAsDialog(rqst, te, parentPanel, eventHandlerContainer, actionOnChangeHandler); 
        });
        popControlsButton.setColor(Colors.Yel);
        return popControlsButton;
    }

    public static void showAsDialog(ImageRenderRequest rqst, TerminalEvent<?> te, ChildComponentManager<?> parentPanel, EventHandlerContainer eventHandlerContainer, EventHandler actionOnChangeHandler) {

        ImageManipulationControls controls = new ImageManipulationControls(new Colors.FGBGColors(Colors.Bla,Colors.On_Whi), rqst);

        EventHandler onDialogClosed = null;

        if (actionOnChangeHandler != null) {
            OnValueChangedEventHandler<ImageRenderRequest> onChangedHandler = controls.onChange(actionOnChangeHandler,  eventHandlerContainer);
            onDialogClosed = (te2) -> eventHandlerContainer.removeEventHandler(onChangedHandler);
        }

        MessageBox.showInfoDialog(te.getEventBus(), parentPanel, "Image Render options",  controls, onDialogClosed, onDialogClosed);

    }


}