package dtb.terminalIO.components.input;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.StringUtils;

public class Button extends BoundedComponent<Button> implements HasValue<Boolean>, ToFocusable, HasHotKey, ParentContainer   {

    String buttonText;
    Composite container;
    TextComponent renderedText;
    String leftSelectedBracket = "[";
    String rightSelectedBracket = "]";
    Boolean pressed = false;
    boolean displayHotKey = false;
    boolean toggleButton = false;
    
    private KeyStroke hotKey;  // not used yet .. soon
    
    @FunctionalInterface
    public static interface Action {
        void buttonPressed( boolean toggleOn);
    }
    
    public Button(Color color, String buttonText, boolean toggle, Action onPressed1) {
        super(color, Rectangle.ZERO);
        this.toggleButton = toggle;
        this.buttonText = buttonText;
        this.renderedText = new TextComponent(color, "");
        this.container = new Composite(color);
        this.container.add(renderedText);
        this.container.setParentContainer(this);
        this.setButtonTextAndResize(buttonText);
    }

    public Button(Color color, String buttonText, boolean toggle) {
        super(color, Rectangle.ZERO);
        this.toggleButton = toggle;
        this.buttonText = buttonText;
        this.renderedText = new TextComponent(color, "");
        this.container = new Composite(color);
        this.container.add(renderedText);
        this.container.setParentContainer(null);
        this.setButtonTextAndResize(buttonText);
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

    public Button setHotKey(KeyStroke hotKey) {
        this.hotKey = hotKey;
        return this;
    }

    public boolean isDisplayHotKey() {
        return displayHotKey && getHotKey() != null;
    }

    public Button setDisplayHotKey(boolean displayHotKey) {
        this.displayHotKey = displayHotKey;
        renderedText.setTextAndResize(getTextForRendering(false));
        return this;
    }

    public void setButtonText(String buttonText) {
        setButtonTextAndResize(buttonText);
    }

    public void resize(Dimension d) {
        if (d != null && d.isSameDimension(this)) {
            return;
        }
        super.resize(d);
        if ( ! d.isSameDimension(this.container)) {
            this.container.resize(d);
        }
    }
    
    private void setButtonTextAndResize(String buttonText) {
        Dimension prevSize = new Dimension.Simple(this.renderedText);
        this.buttonText = buttonText;
        renderedText.setTextAndResize(getTextForRendering(false));
        if ( ! prevSize.isSameDimension(renderedText)) {
            this.container.fullrecalcDim();
            this.resize((Dimension)this.container);
        }
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

         if (isDisplayHotKey()) {
             String hotKeyStr = this.getHotKey().getName();

             outputStr = (this.renderedText.getColor().bold().asString()) + hotKeyStr + this.renderedText.getColor().asString() + " " + outputStr;
         }
         
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
        if ( this.renderedText != null) {
            this.renderedText.setColor(drawingcolor);
        }
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
        eventBus.raiseEvent(  new HasValue.ValueChangedEvent<>(Button.this) );
    }

    
    public Focusable asFocusable() {
        
        ChainedEventHandler chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
        
        chainedHandler.appendEventHandler(new KeyInputEventHandler(Keys.ENTER_KEYSTROKE,(te)->buttonPressed(te.getEventBus()))); 

        if (getHotKey() != null) {
            chainedHandler.appendEventHandler(new KeyInputEventHandler(getHotKey(),(te)->buttonPressed(te.getEventBus())));
        }

        return new DefaultFocusableBehaviour.Simple(this, chainedHandler.asConditionalEventHandler()) {
 
            @Override
            public void setFocus(boolean focus, EventBus eventBus) {
                super.setFocus(focus, eventBus);
                renderedText.setText(getTextForRendering(focus));
                if (focus) {
                    new ShowComponentOnFocus().onFocus(Button.this, eventBus);
                }
            }
            
        };
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.container.getSegmentsBuffer());
    }

    public boolean hasBorder() {
        return this.container.hasBorder();
    }


    public Button setBorder(boolean hasBorder) {
        this.container.setBorder(hasBorder);
        this.container.fullrecalcDim();
        this.resize((Dimension)container);
        this.markAsDirty();
        return this;
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singletonList(this.container);
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
    }

    @Override
    public void childResized(Component<?> child) {
        this.resize((Dimension)child);
    }


}
