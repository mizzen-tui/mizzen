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
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Draggable;
import dtb.terminalIO.geom.CanResize;

public class ResizeAnchor  extends TextComponent implements Focusable , Draggable {

    Focusable meAsFocusable;
    ChainedEventHandler chainedHandler;
    String baseText;
    String draggingText;
    Color nonDraggingColor;
    Color draggingColor;
    Draggable draggableBehaviour;
    boolean dragging;

    public ResizeAnchor(Color color, String text, Color draggingColor, String draggingText, Draggable draggableBehaviour) {
        super(color, text);
        this.setName("ResizeAnchor");
        this.nonDraggingColor = color;
        this.baseText = text;
        this.draggingText = draggingText;
        this.draggingColor = draggingColor != null ? draggingColor : color.bold();
        this.draggableBehaviour = draggableBehaviour;
        this.setFillOnClear(false);
    }

    public ResizeAnchor(Color color, String text, Color draggingColor, String draggingText, CanResize componentToResize) {
        super(color, text);
        this.setName("ResizeAnchor");
        this.nonDraggingColor = color;
        this.baseText = text;
        this.draggingText = draggingText;
        this.draggingColor = draggingColor != null ? draggingColor : color.bold();
        draggableBehaviour = new DragAndResizeComponentBehaviour(componentToResize, this);
        this.setFillOnClear(false);
    }

    public ResizeAnchor(Color color, Color draggingColor, CanResize componentToResize) {
        this(color, "⠶⠶┘", draggingColor, "⠶⠶⠾", componentToResize);
    }

    public ResizeAnchor(Color color, CanResize componentToResize) {
        this(color, null, componentToResize);
    }

    public ResizeAnchor(CanResize componentToResize) {
        this(Colors.DEFAULT_COLOR, componentToResize);
    }


    private boolean tabStop = true;

    /** false: TAB / SHIFT-TAB skip this (it can still be clicked / dragged). Used for window decorations. */
    public ResizeAnchor setTabStop(boolean tabStop) {
        this.tabStop = tabStop;
        return this;
    }

    @Override
    public boolean isTabStop() {
        return tabStop;
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
                setDisplayAttributesOnDragState();
            }
        };
        return meAsFocusable;
    }

    private void setDisplayAttributesOnDragState() {
        if (dragging) {
            this.setColor(draggingColor);
            this.setText(draggingText);
        } else {
            this.setColor(nonDraggingColor);
            this.setText(baseText);
        }
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
        setDisplayAttributesOnDragState();
        return dragging;
    }

    @Override
    public boolean onDragStop(ApplicationMouseEvent appMouseEvent) {
        dragging = false;
        setDisplayAttributesOnDragState();
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
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> asFocusable().setParentFocusManager(fm));
    }

}
