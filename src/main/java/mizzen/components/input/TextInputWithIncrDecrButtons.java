package mizzen.components.input;

import mizzen.components.ComponentWithFocus;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.controlcodes.color.Color;
import mizzen.events.ChainedEventHandler;
import mizzen.events.EventHandler;
import mizzen.events.Registration;

/**
 * "label  <-  value  ->": a text input for a value, with a button each side to step it down and up.
 * Its buttons and input are ordinary TAB stops; onChange hears every change, typed or stepped.
 */
public abstract class TextInputWithIncrDecrButtons<T> extends ComponentWithFocus<HorizontalPanel, TextInputWithIncrDecrButtons<T>>
                                                      implements FormInput<T> {

    ChainedEventHandler chainedEventHandler;
    TextInputForType<T> input;

    public TextInputWithIncrDecrButtons(Color color, String labelText, String downButtonText, String upButtonText) {
        super(color, new HorizontalPanel());

        HorizontalPanel controlsInGrid = getPanel();

        input = buildTextInputForType();
        input.setValue( getValue() );

        Button decrementButton = new Button(color, downButtonText);
        Button incrementButton = new Button(color, upButtonText);

        controlsInGrid.add(new TextComponent(color, labelText));
        controlsInGrid.add(decrementButton);
        controlsInGrid.add(input);
        controlsInGrid.add(incrementButton);

        // emit valueChanged events when the above controls are used
        EventHandler evtHandlerBroadcastValueChanged = (te) -> {
            te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<T>(TextInputWithIncrDecrButtons.this));
        };

        // handle incr/decr requests
        decrementButton.onChange((te)->{
            T value = decr();
            input.setValue(value);
            evtHandlerBroadcastValueChanged.handle(te);
        });

        incrementButton.onChange((te)->{
            T value = incr();
            input.setText(String.valueOf(value));
            evtHandlerBroadcastValueChanged.handle(te);
        });

        input.onChange((te) -> {
            evtHandlerBroadcastValueChanged.handle(te);
        });

        // its "value changed" events reach the onChange handlers through its focus behaviour
        chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
        focus().handleEvents(chainedEventHandler.asConditionalEventHandler());


    }


    private final OnChangeListeners<T> changeListeners = new OnChangeListeners<T>(this);   // onChange: any number of listeners
    public Registration onChange(EventHandler handler) {

        return changeListeners.add(chainedEventHandler, handler);

    }

    public void setValue(T value) {
        this.input.setValue(value);
    }

    @Override
    public T getValue() {
        return this.input.getValue();
    }

    abstract protected TextInputForType<T> buildTextInputForType();

    abstract protected T decr();

    abstract protected T incr();

}
