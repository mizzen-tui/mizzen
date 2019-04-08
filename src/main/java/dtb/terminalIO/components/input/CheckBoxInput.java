package dtb.terminalIO.components.input;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.Focusable;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.StringUtils;

public class CheckBoxInput extends BoundedComponent<CheckBoxInput> implements HasValue<Boolean>, ParentContainer, ToFocusable   {

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
        this(FGBGColors.DEFAULT);
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
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        this.textRenderer.setText(buildRenderText());
        segments.merge(this.textRenderer.getSegmentsBuffer());
    }
    
    protected void init() {
        this.textRenderer = new TextComponent(this.getColor(), this.getRectangle(), buildRenderText());
        this.textRenderer.setParentContainer(this);
    }
    
    
    public Focusable asFocusable() {
        
        return new DefaultFocusableBehaviour.ComponentHiliterFocusableBehaviour(this) {

            @Override
            public void handle(TerminalEvent<?> te) {
                KeyEvent ke = (KeyEvent) te; // TODO:  In the future, this cast might not be valid
                
                if (ke.isKeyStroke( Keys.ENTER_KEYSTROKE )  || ke.isKeyStroke(Keys.SPACE_KEYSTROKE)) {
                    CheckBoxInput.this.setValue(  !  CheckBoxInput.this.getValue()  );  // toggle the value
                    te.getEventBus().raiseEvent(  new HasValue.ValueChangedEvent<>(CheckBoxInput.this) );
                }
            }

            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                KeyEvent ke = (KeyEvent) te; // TODO:  In the future, this cast might not be valid
                
                if (ke.isKeyStroke( Keys.ENTER_KEYSTROKE )  || ke.isKeyStroke(Keys.SPACE_KEYSTROKE)) {
                    return this;
                }
                return null;
            }
            
        };
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.textRenderer);
    }
    
    
}
