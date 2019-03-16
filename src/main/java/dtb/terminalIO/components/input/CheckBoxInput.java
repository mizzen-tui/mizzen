package dtb.terminalIO.components.input;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.FocusManager.Focusable;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.StringUtils;

public class CheckBoxInput extends BoundedComponent<CheckBoxInput> {

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
    
    
    public boolean getValue() {
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
    
    
    public Focusable asFocusable(Position position) {
        return new Focusable() {
            
            boolean hasFocus = false;
            
            Color unhilitedColor = CheckBoxInput.this.getColor();
            Color hilitedColor = CheckBoxInput.this.getColor().bold();

            @Override
            public void handle(TerminalEvent<?> te) {
                KeyEvent ke = (KeyEvent) te; // TODO:  In the future, this cast might not be valid
                
                
                if (ke.isKeyStroke( Keys.ENTER_KEYSTROKE )  || ke.isKeyStroke(Keys.SPACE_KEYSTROKE)) {
                    CheckBoxInput.this.setValue(  !  CheckBoxInput.this.getValue()  );  // toggle the value
                }
                
            }
            
//            private void requestShowCursor() {
//                // todo - need manage a textinput caret (pos) -- not just append
//                eventbus.addEvent(  new  RequestShowCursorEvent(TextInput.this, new Position.Simple(getPosition()).offsetAdd(0, TextInput.this.getText().length())) );
//            }

            @Override
            public boolean hasFocus() {
                return hasFocus;
            }

            @Override
            public void setFocus(boolean focus) {
                this.hasFocus = focus;
                if (focus) {
                    CheckBoxInput.this.setColor(hilitedColor);
                } else {
                    CheckBoxInput.this.setColor(unhilitedColor);
                }
            }

            @Override
            public Position getPosition() {
                return position;
            }

        };
    }
    
    
}
