package mizzen.components.plot.image;

import mizzen.components.input.OnChangeListeners;
import mizzen.components.AbstractComponentWithContainer;
import mizzen.components.ChildComponentManager;
import mizzen.components.composite.Composite;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.HasValue;
import mizzen.components.modal.MessageBox;
import mizzen.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.Registration;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.TerminalEvent;

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

    private final OnChangeListeners<ImageRenderRequest> changeListeners = new OnChangeListeners<ImageRenderRequest>(this);

    /** handler runs when the options change, heard through eventHandlerContainer; the Registration stops it */
    public Registration onChange(EventHandler handler, EventHandlerContainer eventHandlerContainer) {
        return changeListeners.add(eventHandlerContainer, handler);
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
            Registration listening = controls.onChange(actionOnChangeHandler,  eventHandlerContainer);
            onDialogClosed = (te2) -> listening.unregister();           // the dialog's listener goes with the dialog
        }

        MessageBox.showInfoDialog(te.getEventBus(), parentPanel, "Image Render options",  controls, onDialogClosed, onDialogClosed);

    }


}