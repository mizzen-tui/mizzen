package dtb.terminalIO.components.input;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class TextInputForType<T>  extends BoundedComponent<TextInputForType<T>> implements HasValue<T>, ParentContainer, Focusable{

    private TextInput textInput;
    private Function<String,Boolean> validate;
    private Function<String, T> translateToType;
    private ConditionalEventHandler eventHandler;
    
    
    public TextInputForType(Color color, int length, 
                            Function<String,Boolean> validate,
                            Function<String, T> translateToType) {
        super(color, new Dimension.Simple(1, length));
        this.translateToType = translateToType;
        this.validate = validate;
        
        //  build a textbox such that the validate function parameter is injected
        //  into the TextBox's validate method
        this.textInput = new TextInput(color, length) {
            protected boolean validateText(String newText) {
                return TextInputForType.this.validate.apply(newText);
            }
        };
        this.textInput.setParentContainer(this);
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singletonList(this.textInput);
    }
    
    protected void setText(String text) {
        this.textInput.setText(text);
    }

    private ConditionalEventHandler getEventHandler() {
        if (this.eventHandler != null) {
            return this.eventHandler;
        }
        
        // an event handler that routes most incoming events to the internal textInput and
        // additionally captures and 'handles' ValueChangedEvent's emitted from textInput
        
        ChainedEventHandler chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();

        // external listeners don't care if the contents of 'this.textInput' are changed..
        // but the listeners do care when the value of this component has changed
        chainedHandler.appendEventHandler(new OnValueChangedEventHandler<String>(this.textInput) {
            @Override
            public void valueChanged(TerminalEvent<?> te, HasValue<String> source) {
                if (getValue() == null) {
                    return;
                }
                // re-emit a new ValueChangedEvent event indicating that the 'typed' value of this 
                // outer component has changed  
                te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<T>(TextInputForType.this));
            }
        });
        chainedHandler.appendEventHandler(this.textInput);
        
        this.eventHandler = chainedHandler.asConditionalEventHandler();
        
        return this.eventHandler;
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

        // for robustness validate first - even tho' TextInput should have already invalidated
        if (! this.validate.apply(currentText)) {
            return null;
        }

        return this.translateToType.apply(currentText);
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.textInput.getSegmentsBuffer());
    }

}
