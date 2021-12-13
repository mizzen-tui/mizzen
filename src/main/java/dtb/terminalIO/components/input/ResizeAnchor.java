package dtb.terminalIO.components.input;

import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.events.mouse.Draggable;
import dtb.terminalIO.geom.CanResize;

public class ResizeAnchor  extends TextComponent implements Focusable , Draggable {

    Focusable meAsFocusable;
    ChainedEventHandler chainedHandler;
    String baseText;
    String draggingText;
    Color draggingColor;
    Draggable draggableBehaviour;
    boolean dragging;

    public ResizeAnchor(Color color, String text, Color draggingColor, String draggingText, CanResize componentToResize) {
        super(color, hilitedText(text, false));
        this.baseText = text;
        this.draggingText = draggingText;
        this.draggingColor = draggingColor;
        draggableBehaviour = new DragAndResizeComponentBehaviour(componentToResize, this);
    }
    
    // ▮ ▯
    
//    public ResizeAnchor(Color color, Color draggingColor, CanResize componentToResize) {
//        this(color, "┌─┤\n┴─┘", draggingColor, "╔═╣\n╩═╝", componentToResize);
//    }

    public ResizeAnchor(Color color, Color draggingColor, CanResize componentToResize) {
        this(color, "▯▯", draggingColor, "▮▮", componentToResize);
    }

    public ResizeAnchor(Color color, CanResize componentToResize) {
        this(color, null, componentToResize);
    }

    public ResizeAnchor(CanResize componentToResize) {
        this(Colors.DEFAULT_COLOR, componentToResize);
    }


    private Focusable asFocusable() {

        if (meAsFocusable != null) {
            return meAsFocusable;
        }

        chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
        
        meAsFocusable =  new DefaultFocusableBehaviour.HiliteOnFocus(this) {
            @Override
            public void setFocus(boolean focus, EventBus eventBus) {
                super.setFocus(focus, eventBus);
                ResizeAnchor.this.setText(hilitedText(focus?draggingText:baseText, focus));
            }
        };
        return meAsFocusable;
    }

    private static String hilitedText(String baseText, boolean hilited) {
        if (hilited) {
            return baseText;
        }
        return baseText;
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (asFocusable().canHandle(te) != null) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        asFocusable().handle(te);
    }
    
    @Override
    public boolean onDragStart(ApplicationMouseEvent appMouseEvent) {
        dragging = draggableBehaviour.onDragStart(appMouseEvent);
        return dragging;
    }

    @Override
    public boolean onDrag(ApplicationMouseEvent appMouseEvent) {
        dragging = draggableBehaviour.onDrag(appMouseEvent);
        ResizeAnchor.this.setText(hilitedText(dragging?draggingText:baseText, true));
        return dragging;
    }

    @Override
    public boolean onDragStop(ApplicationMouseEvent appMouseEvent) {
        dragging = false;
        return draggableBehaviour.onDragStop(appMouseEvent);
    }

    @Override
    public boolean hasFocus() {
        return asFocusable().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        asFocusable().setFocus(focus, eventBus);
    }

    @Override
    public FocusManager getParentFocusManager() {
        return asFocusable().getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        asFocusable().setParentFocusManager(focusManager);
    }

    
}
