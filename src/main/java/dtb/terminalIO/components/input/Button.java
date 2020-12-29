package dtb.terminalIO.components.input;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.GetComponentAtOffset;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.StringUtils;

public class Button extends BoundedComponent<Button> implements HasValue<Boolean>, 
                                                                Focusable, 
                                                                HasHotKey, 
                                                                Clickable, 
                                                                ParentContainer, 
                                                                GetComponentAtOffset  {

    String buttonText;
    Composite container;  // <-- having a composite for something as simple as a button seems a bit heavy
    TextComponent renderedText;
    String leftSelectedBracket = "[";
    String rightSelectedBracket = "]";
    Boolean pressed = false;
    boolean displayHotKey = false;
    boolean toggleButton = false;
    boolean useCursor = false;
    Focusable meAsFocusable;
    ChainedEventHandler chainedHandler;
    
    private KeyStroke hotKey;
    
    @FunctionalInterface
    public static interface Action {
        void buttonPressed( boolean toggleOn);
    }
    
    public Button(Color color, String buttonText, boolean toggle) {
        super(color, Rectangle.ZERO);
        this.toggleButton = toggle;
        this.buttonText = buttonText;
        this.renderedText = new TextComponent(color, "");
        this.container = new Composite(color);
        this.container.add(renderedText);
        this.container.setParentContainer(this);
        this.setButtonTextAndResize(buttonText);
    }

    public Button(Color color, String buttonText) {
        this(color, buttonText, false);
    }
    
    public Button(String buttonText, boolean toggle) {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED, buttonText, toggle);
    }

    public Button(String buttonText) {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED, buttonText);
    }

    public KeyStroke getHotKey() {
        return hotKey;
    }

    public Button setHotKey(KeyStroke hotKey) {
        this.hotKey = hotKey;
        this.markAsDirty();
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
    
    public String getButtonText() {
        return this.buttonText;
    }

    @Override
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
        this.markAsDirty();
    }
    
    private String lbracket(String brackStr) {
        if (toggleButton) {
            return brackStr + (pressed? "▪" : " ");
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

    public void markAsDirty() {
        if (isDirty()) {
            return;
        }
       super.markAsDirty();
       this.renderedText.markAsDirty();
       this.container.markAsDirty();
    }

    @Override
    public Boolean getValue() {
        return pressed;
    }
 
    public void onClick(EventBus eventBus) {
        if (!hasFocus()) {
            // focus manager should have already put focus here - but just in case ..
            eventBus.raiseEvent(new RequestFocusEvent(this));
        }
        Button.this.setValue( (toggleButton? !pressed :true) );  // toggle the value
        renderedText.setText(getTextForRendering(true));
        eventBus.raiseEvent( new HasValue.ValueChangedEvent<>(Button.this) );
        eventBus.raiseEvent( new RequestUpdateCanvasEvent(this));
    }


    OnValueChangedEventHandler<Boolean> actionOnChangeHandler;

    public void onChange(EventHandler handler) {

        actionOnChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, actionOnChangeHandler, getchainedHandler(), handler);

    }

    public Button setUseCursor(boolean use) {
        this.useCursor = use;
        return this;
    }

    private void requestShowCursorAtComponent(EventBus eventBus) {
        if (!useCursor) return;

        Component.UTIL.requestShowCursorAtComponent(this, new Position.Simple(hasBorder()?1:0,toggleButton?2:1+(hasBorder()?1:0)), eventBus);
    }

    private void requestHideCursor(EventBus eventBus) {
        if (!useCursor) return;
        eventBus.raiseEvent(new RequestHideCursorEvent(this));
    }

    
    private ChainedEventHandler getchainedHandler() {
        if (chainedHandler == null) {
            asFocusable();
        }
        return chainedHandler;
    }

    KeyInputEventHandler keyInputEventHandler;

    public void addKeyStroke(KeyStroke keyStroke) {
        if (keyInputEventHandler == null) {
            asFocusable();
        }
        keyInputEventHandler.addKeyStroke(keyStroke);
    }

    public void addKeyStroke(char ch) {
        addKeyStroke(new KeyStroke(ch));
    }

        
    private Focusable asFocusable() {

        if (meAsFocusable != null) {
            return meAsFocusable;
        }

        chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
        
        KeyStroke[] actionKeys = new KeyStroke[getHotKey()!=null?2:1];
        actionKeys[0] = Keys.ENTER_KEYSTROKE;
        if (getHotKey() != null) {
            actionKeys[1] = getHotKey();
        }

        keyInputEventHandler = new KeyInputEventHandler((te)->onClick(te.getEventBus()),actionKeys);
        chainedHandler.appendEventHandler(keyInputEventHandler); 

        meAsFocusable = new DefaultFocusableBehaviour.Simple(this, chainedHandler.asConditionalEventHandler()) {
 
            @Override
            public void setFocus(boolean focus, EventBus eventBus) {
                super.setFocus(focus, eventBus);
                renderedText.setText(getTextForRendering(focus));
                if (focus) {
                    new ShowComponentOnFocus().onFocus(Button.this, eventBus);
                    requestShowCursorAtComponent(eventBus);
                } else {
                    requestHideCursor(eventBus);
                }
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
    public boolean hasFocus() {
        return asFocusable().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        asFocusable().setFocus(focus, eventBus);
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.container.getSegmentsBuffer());
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        Component<?> component = this.container.getComponentAtOffset(offset);
        if (component == this.container) {
            return this;
        }
        return component;
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
