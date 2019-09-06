package dtb.terminalIO.components.input;

import dtb.terminalIO.components.AbstractComponentWithContainerAndFocusManager;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;

public class RockerButton extends AbstractComponentWithContainerAndFocusManager<ButtonBar>
        implements HasHotKey, Clickable, HasValue<Boolean> {

    boolean toggleValue = false;

    String labelText;
    TextComponent label;
    ButtonBar buttons;
    Button onButton;
    Button offButton;

    private KeyStroke hotKey;
    private KeyInputEventHandler hotKeyHandler;

    public RockerButton(Color color, String label, String onText, String offText, boolean value) {
        super(color);

        onButton = new Button(onText);
        onButton.setActionHandler((te) -> toggleState(true, te.getEventBus()));

        offButton = new Button(offText);
        offButton.setActionHandler((te) -> toggleState(false, te.getEventBus()));

        buttons = new ButtonBar(color);
        buttons.setNoBorderChars();
        buttons.setHorizontalSpacer(1);

        this.labelText = label;
        this.label = new TextComponent(color, label);

        buttons.addComponent(this.label);
        buttons.add(onButton);
        buttons.add(new TextComponent("/"));
        buttons.add(offButton);

        buttons.setParentContainer(this);
        this.setLabelText();
        this.toggleValue = value;
        
        this.toggleState(value, EventBus.STUBBED);
    }
    
    public RockerButton(Color color, String label, String onText, String offText) {
        this(color,  label, onText, offText, false);
    }
    
    public RockerButton(Color color, String label, boolean value) {
        this(color, label, "ON", "OFF", value);
    }

    public RockerButton(Color color, String label) {
        this(color, label, false);
    }

    private void setLabelText() {
        if (this.getHotKey() != null) {
            this.label.setTextAndResize(this.hotKey.getName()+":" + " " + this.labelText  );
        } else {
            this.label.setTextAndResize( this.labelText );
        }
        this.buttons.fullrecalcDim();
    }
    

    private void toggleState(boolean state, EventBus eventBus) {
        this.toggleValue = state;
        if (state) {
            getFocusManager().setChildAsCurrentFocus(onButton, eventBus);
        } else {
            getFocusManager().setChildAsCurrentFocus(offButton, eventBus);
        }
        this.markAsDirty();
        eventBus.raiseEvent( new HasValue.ValueChangedEvent<>(this) );
        eventBus.raiseEvent( new RequestUpdateCanvasEvent(this));
    }

    OnValueChangedEventHandler<Boolean> actionOnChangeHandler;

    public void setActionHandler(EventHandler handler) {

        if (actionOnChangeHandler != null) {
            this.buttons.removeEventHandler(actionOnChangeHandler);
        }

        if (handler == null) {
            return;
        }
        actionOnChangeHandler = new HasValue.OnValueChangedEventHandler<Boolean>(this) {
            @Override
            public void valueChanged(TerminalEvent<?> te, HasValue<Boolean> source) {
                handler.handle(te);
            }
        };
        this.buttons.appendEventHandler(actionOnChangeHandler);
    }

    @Override
    public void onClick(EventBus eventBus) {
        toggleState(!this.toggleValue, eventBus);
    }

    @Override
    protected ButtonBar getContainer() {
        return buttons;
    }

    @Override
    protected FocusManager getFocusManager() {
        return buttons;
    }

    public RockerButton setHotKey(KeyStroke hotKey) {
        if (this.hotKeyHandler != null) {
            this.buttons.removeEventHandler(hotKeyHandler);
        }
        this.hotKey = hotKey;
        if (this.hotKey != null) {
            this.hotKeyHandler = new KeyInputEventHandler(this.hotKey, (te) -> onClick(te.getEventBus()));
            this.buttons.appendEventHandler(this.hotKeyHandler);
        }
        setLabelText();
        return this;
    }

    @Override
    public KeyStroke getHotKey() {
        return this.hotKey;
    }

    @Override
    public Boolean getValue() {
        return toggleValue;
    }

}