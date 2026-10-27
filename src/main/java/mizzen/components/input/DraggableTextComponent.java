package mizzen.components.input;

import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.controlcodes.color.Color;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Draggable;

public class DraggableTextComponent extends TextComponent implements Focusable , Draggable {

    Focusable meAsFocusable;
    String baseText;
    String draggingText;
    Color nonDraggingColor;
    Color draggingColor;
    Draggable draggableBehaviour;
    Boolean hasColorInText;
    boolean dragging;

    public DraggableTextComponent(Color color, String text, Color draggingColor, String draggingText, Draggable draggableBehaviour) {
        super(color, text);
        this.nonDraggingColor = color;
        this.baseText = text;
        this.draggingText = draggingText != null ? draggingText : this.baseText;
        this.draggingColor = draggingColor != null ? draggingColor : color;
        this.draggableBehaviour = draggableBehaviour;
    }

    public DraggableTextComponent(Color color, String text, Color draggingColor, String draggingText, Component<?> componentToDrag) {
        super(color, text);
        this.nonDraggingColor = color;
        this.baseText = text;
        this.draggingText = draggingText != null ? draggingText : this.baseText;
        this.draggingColor = draggingColor != null ? draggingColor : color;
        this.draggableBehaviour = new DragAndMoveComponentBehaviour(componentToDrag != null? componentToDrag: this);
    }

    public DraggableTextComponent(Color color, String text, Color draggingColor, String draggingText) {
        this(color, text, null, null, (Component<?>)null);
    }

    public DraggableTextComponent(Color color, String text) {
        this(color, text, null, null);
    }

    public DraggableTextComponent(Color color, String text, Component<?> componentToDrag) {
        this(color, text, null, null, componentToDrag);
    }

    public void setcomponentToDrag(Component<?> componentToDrag) {
        this.draggableBehaviour = new DragAndMoveComponentBehaviour(componentToDrag != null? componentToDrag: this);
    }
    
    public Draggable getDraggableBehaviour() {
        return draggableBehaviour;
    }

    public void setDraggableBehaviour(Draggable draggableBehaviour) {
        this.draggableBehaviour = draggableBehaviour;
    }

    public DraggableTextComponent freezeColumn() {
        ((DragAndMoveComponentBehaviour)this.draggableBehaviour).freezeColumn();
        return this;
    }

    public DraggableTextComponent freezeRow() {
        ((DragAndMoveComponentBehaviour)this.draggableBehaviour).freezeRow();
        return this;
    }

    public DraggableTextComponent unFreeze() {
        ((DragAndMoveComponentBehaviour)this.draggableBehaviour).unFreeze();
        return this;
    }


    private boolean tabStop = true;

    /** false: TAB / SHIFT-TAB skip this (it can still be clicked / dragged). Used for window decorations. */
    public DraggableTextComponent setTabStop(boolean tabStop) {
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
    public boolean onDragStart(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
        this.dragging = draggableBehaviour.onDragStart(appMouseEventRelativeToCanvas);
        setDisplayAttributesOnDragState();
        return this.dragging;
    }

    @Override
    public boolean onDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
        this.dragging = draggableBehaviour.onDrag(appMouseEventRelativeToCanvas);
        setDisplayAttributesOnDragState();
        return this.dragging;
    }

    @Override
    public boolean onDragStop(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
        this.dragging = false;
        draggableBehaviour.onDragStop(appMouseEventRelativeToCanvas);
        setDisplayAttributesOnDragState();
        return true;
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
    
    

    public String getBaseText() {
		return baseText;
	}

	public void setBaseText(String baseText) {
		if (this.baseText != null && this.baseText.equals(this.draggingText)) {
			this.setDraggingText(baseText);
		}
		this.baseText = baseText;
		if ( ! this.dragging) {
			this.setTextAndResize(baseText);
		}
	}

	public String getDraggingText() {
		return draggingText;
	}

	public void setDraggingText(String draggingText) {
		this.draggingText = draggingText;
		if (this.dragging) {
			this.setTextAndResize(draggingText);
		}
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
