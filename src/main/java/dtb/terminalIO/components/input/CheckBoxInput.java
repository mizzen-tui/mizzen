package dtb.terminalIO.components.input;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusBehaviour;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.Focusable.FocusableFromParts;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.AsKeyboardHandler;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyboardHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.StringUtils;

public class CheckBoxInput extends BoundedComponent<CheckBoxInput> 
             implements HasValue<Boolean>, ParentContainer, ToFocusable, Clickable, AsKeyboardHandler   {

    private TextComponent textRenderer;

    boolean value = false;

    public CheckBoxInput(Color color, int width) {
        super(color, new Dimension.Simple(1, width));
        init();
    }

    public CheckBoxInput(Color color) {
        this(color, 5); // [x]
    }

    public CheckBoxInput() {
        this(Colors.DEFAULT_COLOR);
    }

    public Boolean getValue() {
        return value;
    }
    
    public void markAsDirty() {
        super.markAsDirty();
        if (this.textRenderer != null) {
            this.textRenderer.markAsDirty();
        }
    }

    public boolean isDirty() {
        return super.isDirty() || (this.textRenderer != null && this.textRenderer.isDirty());
    }

    public void setValue(boolean value) {
        this.value = value;
        this.markAsDirty();
    }

    String buildRenderText() {
        String valueStr = (value) ? "x" : " ";
        String output = " [" + valueStr + "] ";
        if (output.length() < this.getWidth()) {
            // right align -- for now - add options later ..
            output =  StringUtils.repeat(' ', this.getWidth() - output.length());
        }
        return output;
    }

    @Override
    public CheckBoxInput setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        if (this.textRenderer  != null) {
            this.textRenderer.setColor(drawingcolor);
            this.textRenderer.markAsDirty();
        }
        this.markAsDirty();
        return this;
    }

    public CheckBoxInput setEmptyColor(Color emptyColor) {
        super.setEmptyColor(emptyColor);
        if (this.textRenderer != null) {
            this.textRenderer.setEmptyColor(emptyColor);
        }
        return this;
    }
    
    public CheckBoxInput setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        if (this.textRenderer != null) {
            this.textRenderer.setEmptyChar(emptyChar);
        }
        return this;
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.textRenderer) {
            return Position.ZERO;
        }
        return null;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }
    
    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        this.textRenderer.setText(buildRenderText());
        mySegments.clearAndMerge(this.textRenderer.getSegmentsBuffer().getSegments());
    }

    protected void init() {
        this.textRenderer = new TextComponent(this.getColor(), this.getRectangle(), buildRenderText());
        this.textRenderer.setParentContainer(this);
    }
    

    private void requestShowCursorAtComponent(EventBus eventBus) {
        Component.UTIL.requestShowCursorAtComponent(this, new Position.Simple(0,2), eventBus);
    }

    private void requestHideCursor(EventBus eventBus) {
        eventBus.raiseEvent(new RequestHideCursorEvent(this));
    }

    protected void checkBoxPressed(EventBus eventBus) {
        boolean newValue = !  CheckBoxInput.this.getValue();  // toggle the value
        CheckBoxInput.this.setValue(  newValue  );  
        eventBus.raiseEvent(new HasValue.ValueChangedEvent<>(CheckBoxInput.this) );
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
    }
    

    public void onClick(EventBus eventBus) {
        if (!asFocusable().hasFocus()) {
            // focus manager should have already put focus here - but just in case ..
            eventBus.raiseEvent(new RequestFocusEvent(asFocusable()));
        }
        checkBoxPressed(eventBus);
    }

    /** ENTER or SPACE toggles the box */
    private final KeyboardHandler keys = AsKeyboardHandler.UTIL.toKeyboardHandler(
            new KeyInputEventHandler((te) -> checkBoxPressed(te.getEventBus()), Keys.ENTER_KEYSTROKE, Keys.SPACE_KEYSTROKE));

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return keys;
    }

    OnValueChangedEventHandler<Boolean> actionOnChangeHandler;

    public void onChange(EventHandler handler) {

        actionOnChangeHandler = HasValue.UTIL.wrapHandlerIntoOnValueChangedHandler(this, handler);

    }

    Focusable meAsFocusable;

    public Focusable asFocusable() {

        if (meAsFocusable != null) {
            return meAsFocusable;
        }

        Predicate<TerminalEvent<?>> canHandleOnChangeEvent = (te)-> {
            if (this.actionOnChangeHandler == null) {
                return false;
            }
            if (this.actionOnChangeHandler.canHandle(te)!=null) {
                return true;
            }
            return false;
        };

        // keys: see asKeyboardHandler. Here only this box's own "value changed" event, for onChange
        ConditionalEventHandler evtHandler = new ConditionalEventHandler() {

            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                return canHandleOnChangeEvent.test(te) ? this : null;
            }

            @Override
            public void handle(TerminalEvent<?> te) {
                if (canHandleOnChangeEvent.test(te)) {
                    actionOnChangeHandler.handle(te);
                }
            }

        };

        FocusBehaviour focusBehaviour = new DefaultFocusableBehaviour.ComponentHiliterFocusableBehaviour(this) {

            @Override
            public void setFocus(boolean focus, EventBus eventBus) {
                super.setFocus(focus, eventBus);
                if (focus) {
                    new ShowComponentOnFocus().onFocus(CheckBoxInput.this, eventBus);
                    requestShowCursorAtComponent(eventBus);
                } else {
                    requestHideCursor(eventBus);
                }
            }

            public String toString() {
                return "ToFocusable(CheckBoxInput, value="+CheckBoxInput.this.getValue()+")";
            }

        };
        
        meAsFocusable =   new FocusableFromParts(this, focusBehaviour, evtHandler);
        
        return meAsFocusable;
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.textRenderer);
    }

}
