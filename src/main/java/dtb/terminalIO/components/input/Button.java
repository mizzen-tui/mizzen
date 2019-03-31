package dtb.terminalIO.components.input;

import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.Focusable;
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.util.StringUtils;

public class Button extends Composite implements HasValue<Boolean>, AsFocusable.ToFocusable, HasHotKey   {

    String buttonText;
    TextComponent renderedText;
    String leftSelectedBracket = "[";
    String rightSelectedBracket = "]";
    Boolean pressed = false;
    boolean toggleButton = false;
    
    private KeyStroke hotKey;  // not used yet .. soon
    
    @FunctionalInterface
    public static interface Action {
        void buttonPressed( boolean toggleOn);
    }
    
    public Button(Color color, String buttonText, boolean toggle, Action onPressed) {
        super(color);
        this.toggleButton = toggle;
        this.buttonText = buttonText;
        renderedText = new TextComponent(color, getTextForRendering(false));
        this.add(renderedText);
    }

    public Button(Color color, String buttonText, boolean toggle) {
        super(color);
        this.toggleButton = toggle;
        this.buttonText = buttonText;
        renderedText = new TextComponent(color, getTextForRendering(false));
        this.add(renderedText);
    }

    public Button(Color color, String buttonText) {
        this(color, buttonText, false);
    }
    
    public Button(String buttonText, boolean toggle) {
        this(FGBGColors.NO_COLOR_SPECIFIED, buttonText, toggle);
    }

    public Button(String buttonText) {
        this(FGBGColors.NO_COLOR_SPECIFIED, buttonText);
    }

    public KeyStroke getHotKey() {
        return hotKey;
    }

    public void setHotKey(KeyStroke hotKey) {
        this.hotKey = hotKey;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
        renderedText.setTextAndResize(getTextForRendering(false));
    }
    
    private String lbracket(String brackStr) {
        if (toggleButton) {
            return brackStr + (pressed? "*" : " ");
        }
        // not a toggle button
        return brackStr;
    }

    private String rbracket(String brackStr) {
        if (toggleButton) {
            return (pressed? "" : "") + brackStr;
        }
        // not a toggle button
        return brackStr;
    }
    
    private String getTextForRendering(boolean hasFocus) {
         String outputStr = this.buttonText;
         if (hasFocus) {
             outputStr = lbracket(leftSelectedBracket) + outputStr + rbracket(rightSelectedBracket);
         } else {
             // output black brackets ..
             outputStr = lbracket(StringUtils.repeat(' ', leftSelectedBracket != null ? leftSelectedBracket.length():0)) + 
                         outputStr + 
                         rbracket(StringUtils.repeat(' ', rightSelectedBracket != null ? leftSelectedBracket.length():0));
         }
         return outputStr;
    }
    
    @Override
    public Button setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        this.renderedText.setColor(drawingcolor);
        return this;
    }

    public void setValue(boolean value) {
        this.pressed = value;
        this.markAsDirty();
    }
    
    
    @Override
    public Boolean getValue() {
        return pressed;
    }
    
    
    protected void buttonPressed(EventBus eventBus) {
        Button.this.setValue( (toggleButton? !pressed :true) );  // toggle the value
        renderedText.setText(getTextForRendering(true));
        eventBus.addEvent(  new HasValue.ValueChangedEvent<>(Button.this) );
    }

    
    public Focusable asFocusable(EventBus eventBus) {
        
        ChainedEventHandler chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
        
        chainedHandler.addEventHandler(new KeyInputEventHandler(Keys.ENTER_KEYSTROKE,(te)->buttonPressed(eventBus))); 

        if (getHotKey() != null) {
            chainedHandler.addEventHandler(new KeyInputEventHandler(getHotKey(),(te)->buttonPressed(eventBus)));
        }

        
        return new AsFocusable.Simple(this, eventBus, chainedHandler.asConditionalEventHandler()) {
 
            @Override
            public void setFocus(boolean focus) {
                super.setFocus(focus);
                renderedText.setText(getTextForRendering(focus));
            }
            
        };
    }

    
}
