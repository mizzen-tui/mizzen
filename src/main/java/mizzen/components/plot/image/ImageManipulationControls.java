package mizzen.components.plot.image;

import java.util.ArrayList;
import java.util.List;

import mizzen.app.TUIApp;
import mizzen.components.input.OnChangeListeners;
import mizzen.components.AbstractComponentWithContainer;
import mizzen.components.ChildComponentManager;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.DisplayableValue;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.HasValue;
import mizzen.components.input.Slider;
import mizzen.components.modal.Dialog;
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
        super(color, new VerticalPanel());

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

        controls.add(labelled("Render as  ", imageRendererTypeDDLB));

        DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode> colorMapDDLB = new DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode>(ImageViewerFactory.colorMap());
        colorMapDDLB.setName("colorMapDDLB");
        colorMapDDLB.setValue( ImageManipulationControls.this.rqst.getColorMap() );
        colorMapDDLB.onChange((te)->{
            rqst.setColorMap( colorMapDDLB.getValue()  );
            te.getEventBus().raiseEvent( new HasValue.ValueChangedEvent<>(ImageManipulationControls.this) );
            te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(ImageManipulationControls.this));
        });

        colorMapDDLB.setColor(color);

        controls.add(labelled("Colours    ", colorMapDDLB));

        controls.add(plotChoice(color));
    }

    // what to plot: a kind (always, white only, ... lighter than, darker than) and, for lighter / darker than, the grey
    // level on a slider. A predicate the app made itself shows as "as given" until another kind is picked.
    private enum PlotKind {
        ALWAYS("always"), WHITE("white only"), BLACK("black only"), LIGHTER("lighter than"), DARKER("darker than"),
        GIVEN("as given");

        final String label;

        PlotKind(String label) {
            this.label = label;
        }
    }

    private HorizontalPanel plotChoice(Color color) {
        ImageViewerFactory.RGBPlotPredicate now = rqst.getToPlotOrNot();
        PlotKind kind = kindOf(now);
        int grey = now instanceof ImageViewerFactory.GreyLevel ? ((ImageViewerFactory.GreyLevel) now).grey : 125;

        List<DisplayableValue<PlotKind>> kinds = new ArrayList<DisplayableValue<PlotKind>>();
        for (PlotKind k : PlotKind.values()) {
            if (k != PlotKind.GIVEN || kind == PlotKind.GIVEN) {
                kinds.add(new DisplayableValue.SimpleDisplayValue<PlotKind>(k.label, k));
            }
        }
        @SuppressWarnings("unchecked")
        DropdownListBox<PlotKind> kindDDLB = new DropdownListBox<PlotKind>(kinds.toArray(new DisplayableValue[0]));
        kindDDLB.setName("plotKindDDLB");
        kindDDLB.setValue(kind);
        kindDDLB.setColor(color);

        Slider level = new Slider(0, 255).value(grey);
        level.setName("plotLevel");

        HorizontalPanel row = labelled("Plot       ", kindDDLB);
        row.add(new TextComponent(" "));
        row.add(level);
        showLevelFor(kind, row, level);

        kindDDLB.onChange((te) -> {
            showLevelFor(kindDDLB.getValue(), row, level);
            plotChanged(kindDDLB.getValue(), level.getValue(), te);
        });
        level.onChange((te) -> plotChanged(kindDDLB.getValue(), level.getValue(), te));
        return row;
    }

    private static PlotKind kindOf(ImageViewerFactory.RGBPlotPredicate predicate) {
        if (predicate == null || predicate == ImageViewerFactory.PLOT_ALWAYS) {
            return PlotKind.ALWAYS;
        } else if (predicate == ImageViewerFactory.PLOT_WHITE_ONLY) {
            return PlotKind.WHITE;
        } else if (predicate == ImageViewerFactory.PLOT_BLACK_ONLY) {
            return PlotKind.BLACK;
        } else if (predicate instanceof ImageViewerFactory.GreyLevel) {
            return ((ImageViewerFactory.GreyLevel) predicate).lighter ? PlotKind.LIGHTER : PlotKind.DARKER;
        }
        return PlotKind.GIVEN;
    }

    private static void showLevelFor(PlotKind kind, HorizontalPanel row, Slider level) {
        if (kind == PlotKind.LIGHTER || kind == PlotKind.DARKER) {
            row.show(level);
        } else {
            row.hide(level);
        }
    }

    private void plotChanged(PlotKind kind, int grey, TerminalEvent<?> te) {
        switch (kind) {
            case ALWAYS:  rqst.setToPlotOrNot(ImageViewerFactory.PLOT_ALWAYS); break;
            case WHITE:   rqst.setToPlotOrNot(ImageViewerFactory.PLOT_WHITE_ONLY); break;
            case BLACK:   rqst.setToPlotOrNot(ImageViewerFactory.PLOT_BLACK_ONLY); break;
            case LIGHTER: rqst.setToPlotOrNot(ImageViewerFactory.plotLighterThan(grey)); break;
            case DARKER:  rqst.setToPlotOrNot(ImageViewerFactory.plotDarkerThan(grey)); break;
            case GIVEN:   return;                                     // still the app's own: nothing changed
        }
        te.getEventBus().raiseEvent( new HasValue.ValueChangedEvent<>(ImageManipulationControls.this) );
        te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(ImageManipulationControls.this));
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
        return popControlsButton;
    }

    public static void showAsDialog(ImageRenderRequest rqst, TerminalEvent<?> te, ChildComponentManager<?> parentPanel, EventHandlerContainer eventHandlerContainer, EventHandler actionOnChangeHandler) {

        ImageManipulationControls controls = new ImageManipulationControls(rqst);   // no colour: the dialog's theme

        Registration listening = actionOnChangeHandler == null ? Registration.NONE
                : controls.onChange(actionOnChangeHandler, eventHandlerContainer);
        // over the app's whole screen when there is one, so it can be dragged off the picture it changes; shown on the
        // picture's own small panel it would grow that panel (and move what is laid out from its edge)
        TUIApp app = parentPanel instanceof Component ? TUIApp.of((Component<?>) parentPanel) : null;
        Dialog.info("Image Render options").draggable().add(controls).ok()
              .onClose(listening::unregister)                                   // the dialog's listener goes with the dialog
              .show(te.getEventBus(), app != null ? app.getMainApplicationPanel() : parentPanel);

    }


    // a label to the left of input, on one line
    private static HorizontalPanel labelled(String label, Component<?> input) {
        return new HorizontalPanel().add(new TextComponent(label)).add(input);
    }
}
