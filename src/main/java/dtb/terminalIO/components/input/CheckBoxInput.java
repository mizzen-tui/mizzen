package dtb.terminalIO.components.input;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.Focusable;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.StringUtils;

public class CheckBoxInput extends BoundedComponent<CheckBoxInput> implements HasValue<Boolean> {

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
    

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        this.textRenderer.setText(buildRenderText());
        segments.merge(this.textRenderer.getSegmentsBuffer());
    }
    
    protected void init() {
        this.textRenderer = new TextComponent(this.getColor(), this.getRectangle(), buildRenderText());
    }
    
    
    public Focusable asFocusable(Position position, EventBus eventBus) {
        
        return new AsFocusable(this, position, eventBus) {

            @Override
            public void handle(TerminalEvent<?> te) {
                KeyEvent ke = (KeyEvent) te; // TODO:  In the future, this cast might not be valid
                
                if (ke.isKeyStroke( Keys.ENTER_KEYSTROKE )  || ke.isKeyStroke(Keys.SPACE_KEYSTROKE)) {
                    CheckBoxInput.this.setValue(  !  CheckBoxInput.this.getValue()  );  // toggle the value
                    eventBus.addEvent(  new HasValue.ValueChangedEvent<>(CheckBoxInput.this) );
                }
            }
            
        };
    }
    
    
}
