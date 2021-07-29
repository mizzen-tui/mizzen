package dtb.terminalIO.components.input;

import dtb.terminalIO.components.AbstractWrappedComponentWithFocusManager;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.mouse.Clickable;

public class RockerButton extends    AbstractWrappedComponentWithFocusManager<ButtonBar> 
                          implements HasHotKey, 
                                     Clickable, 
                                     HasValue<Boolean>,
                                     FocusManager {

    private boolean toggleValue = false;

    private String        labelText;
    private TextComponent label;
    private Button        onButton;
    private Button        offButton;
    private String        onText;
    private String        offText;

    private KeyStroke hotKey;
    private KeyInputEventHandler hotKeyHandler;

    public RockerButton(Color color, String label, String onText, String offText, boolean value) {
        super(color, new ButtonBar(color));

        this.onText = onText;
        this.offText = offText;
        
        onButton = new Button(this.onText);
        onButton.onChange((te) -> toggleState(true, te.getEventBus()));

        offButton = new Button(this.offText);
        offButton.onChange((te) -> toggleState(false, te.getEventBus()));

        getWrappedComponent().setNoBorderChars();
        getWrappedComponent().setHorizontalSpacer(0);

        if (!label.endsWith(" ")) {
            label = label + " ";
        }
        this.labelText = label;
        this.label = new TextComponent(color, label);

        getWrappedComponent().addComponent(this.label);
        getWrappedComponent().addComponent(onButton);
        getWrappedComponent().addComponent(new TextComponent("/"));
        getWrappedComponent().addComponent(offButton);

        this.setLabelText();
        this.toggleValue = value;
        
        this.toggleState(value, new EventBus.STUBBED());
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
       // this.fullrecalcDim();
    }
    

    private void toggleState(boolean state, EventBus eventBus) {
        this.toggleValue = state;
        if (state) {
            this.onButton. setButtonText("<" + this.onText+">");
            this.offButton.setButtonText(" " + this.offText+" ");
            getFocusManager().setChildAsCurrentFocus(onButton, eventBus);
        } else {
            this.onButton. setButtonText(" " + this.onText+" ");
            this.offButton.setButtonText("<" + this.offText+">");
            getFocusManager().setChildAsCurrentFocus(offButton, eventBus);
        }
        this.markAsDirty();
        eventBus.raiseEvent( new HasValue.ValueChangedEvent<>(this) );
        eventBus.raiseEvent( new RequestUpdateCanvasEvent(this));
    }

    OnValueChangedEventHandler<Boolean> actionOnChangeHandler;

    public void onChange(EventHandler handler) {

        actionOnChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, actionOnChangeHandler, this.getWrappedComponent(), handler);

    }

    @Override
    public void onClick(EventBus eventBus) {
        toggleState(!this.toggleValue, eventBus);
    }

    protected FocusManager getFocusManager() {
        return getWrappedComponent();
    }

    public RockerButton setHotKey(KeyStroke hotKey) {
        if (this.hotKeyHandler != null) {
            this.getWrappedComponent().removeEventHandler(hotKeyHandler);
        }
        this.hotKey = hotKey;
        if (this.hotKey != null) {
            this.hotKeyHandler = new KeyInputEventHandler(this.hotKey, (te) -> onClick(te.getEventBus()));
            this.getWrappedComponent().appendEventHandler(this.hotKeyHandler);
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


    @Override
    public void childHasCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        // TODO Auto-generated method stub
        
    }



}