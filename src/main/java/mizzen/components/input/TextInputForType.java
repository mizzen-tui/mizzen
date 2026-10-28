package mizzen.components.input;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import mizzen.components.BoundedComponent;
import mizzen.components.Component;
import mizzen.components.ParentContainer;
import mizzen.controlcodes.color.Color;
import mizzen.events.ChainedEventHandler;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.Registration;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.StringUtils;

public class TextInputForType<T>  extends BoundedComponent<TextInputForType<T>> implements FormInput<T>, ParentContainer, Focusable {

    private TextInput textInput;
    private Function<String,Boolean> validate;
    private Function<String, T> translateToType;
    private Function<T, String> translateFromType;
    private ConditionalEventHandler eventHandler;
    private ChainedEventHandler chainedHandler;

    public TextInputForType(Color color, int length, 
                            Function<String,Boolean> validate,
                            Function<String, T> translateToType,
                            Function<T, String> translateFromType) {
        super(color, new Dimension.Simple(1, length));
        this.translateToType = translateToType;
        this.translateFromType = translateFromType;
        this.validate = validate;

        //  build a textbox such that the validate function parameter is injected
        //  into the TextBox's validate method
        this.textInput = new TextInput(color, length) {
            protected boolean validateText(String newText) {
                return TextInputForType.this.validate.apply(newText);
            }
        };

        this.textInput.setParentContainer(this);

        this.textInput.onChange((te)->{
            if (getValue() == null) {
                return;
            }
            // emit a new ValueChangedEvent event indicating that the 'typed' value of this 
            // outer component has changed due to the inner TextInput component changing
            te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<T>(TextInputForType.this));
        });
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singletonList(this.textInput);
    }

    public void setText(String text) {
        this.textInput.setText(text);
    }

    private final OnChangeListeners<T> changeListeners = new OnChangeListeners<T>(this);   // onChange: any number of listeners
    public Registration onChange(EventHandler consumeChange) {

        return changeListeners.add(getChainedEventHandler(), consumeChange);

    }

    private ConditionalEventHandler getEventHandler() {
        if (this.eventHandler != null) {
            return this.eventHandler;
        }

        this.eventHandler = getChainedEventHandler().asConditionalEventHandler();

        return this.eventHandler;
    }

    private ChainedEventHandler getChainedEventHandler() {
        if (this.chainedHandler != null) {
            return this.chainedHandler;
        }
        this.chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
        chainedHandler.appendEventHandler(this.textInput);
        return this.chainedHandler;
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (getEventHandler().canHandle(te) != null) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        this.getEventHandler().handle(te);
    }

    @Override
    public boolean hasFocus() {
        return this.textInput.hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        this.textInput.setFocus(focus, eventBus);
    }

    @Override
    public void unFocus(Focusable newFocusable, EventBus eventBus) {
        this.textInput.unFocus(newFocusable, eventBus);
    }
    
    @Override
    public void focus(Focusable oldFocusable, EventBus eventBus) {
        this.textInput.focus(oldFocusable, eventBus);
    }
    
    @Override
    public FocusManager getParentFocusManager() {
        return this.textInput.getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> this.textInput.setParentFocusManager(fm));
    }
    
    
    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    @Override
    public T getValue() {

        String currentText = this.textInput.getText();
        if (currentText == null || currentText.trim().length()==0) {
            return null;
        }

        currentText = currentText.trim();

        // for robustness validate first - even tho' TextInput should have already been validated
        if (! this.validate.apply(currentText)) {
            return null;
        }

        return this.translateToType.apply(currentText);
    }

    @Override
    /** the value as text; null: empty (getValue gives null back, not a made-up 0) */
    public void setValue(T value) {
        if (value == null) {
            setText("");
            return;
        }
        String currentText = translateFromType.apply(value);
        if (! this.validate.apply(currentText)) {
            return;
        }
        setText(currentText);
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        mySegments.clearAndMerge(this.textInput.getSegmentsBuffer().getSegments());
    }

    /**
     * Its text input is redrawn with it, as AbstractComponentWrappper does for what it wraps. draw()
     * takes the input's segments set itself (no copy, for speed), and that set is cleared before each
     * redraw of this: redrawn alone - e.g. after show() - this would empty the input's segments while
     * the input still counts as drawn, and show nothing.
     */
    @Override
    public void markAsDirty() {
        if (textInput != null && !textInput.isDirty()) {   // null: still in the constructor
            textInput.markAsDirty();
        }
        super.markAsDirty();
    }

    public String toStringDetails() {
        return "[value=" + StringUtils.truncate(String.valueOf(this.getValue()),10)+"]";
    }

}
