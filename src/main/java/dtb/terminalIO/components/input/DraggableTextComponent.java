package dtb.terminalIO.components.input;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Draggable;

public class DraggableTextComponent extends TextComponent implements Focusable , Draggable {

    Focusable meAsFocusable;
    String baseText;
    String draggingText;
    Color draggingColor;
    Draggable draggableBehaviour;
    Boolean hasColorInText;

    public DraggableTextComponent(Color color, String text, Color draggingColor, String draggingText, Component<?> componentToDrag) {
        super(color, text);
        this.baseText = text;
        this.draggingText = draggingText;
        this.draggingColor = draggingColor;
        draggableBehaviour = new DragAndMoveComponentBehaviour(componentToDrag != null? componentToDrag: this);
    }

    public DraggableTextComponent(Color color, String text, Color draggingColor, String draggingText) {
        this(color, text, null, null, null);
    }

    public DraggableTextComponent(Color color, String text) {
        this(color, text, null, null);
    }

    public DraggableTextComponent(Color color, String text, Component<?> componentToDrag) {
        this(color, text, null, null, componentToDrag);
    }


    private Focusable asFocusable() {

        if (meAsFocusable != null) {
            return meAsFocusable;
        }

        meAsFocusable =  new DefaultFocusableBehaviour.HiliteOnFocus(this) {
            @Override
            public void setFocus(boolean focus, EventBus eventBus) {
                super.setFocus(focus, eventBus);
                //DraggableTextComponent.this.setText(baseText);
            }
        };
        return meAsFocusable;
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
        return draggableBehaviour.onDragStart(appMouseEvent);
    }

    @Override
    public boolean onDrag(ApplicationMouseEvent appMouseEvent) {
        return draggableBehaviour.onDrag(appMouseEvent);
    }

    @Override
    public boolean onDragStop(ApplicationMouseEvent appMouseEvent) {
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
