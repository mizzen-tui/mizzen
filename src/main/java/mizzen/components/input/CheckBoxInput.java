package mizzen.components.input;

import java.util.Collection;
import java.util.Collections;
import mizzen.components.BoundedComponent;
import mizzen.components.Component;
import mizzen.components.ParentContainer;
import mizzen.components.TextComponent;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.Registration;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.Clickable;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.StringUtils;

public class CheckBoxInput extends BoundedComponent<CheckBoxInput> 
             implements FormInput<Boolean>, ParentContainer, ToFocusable, Clickable, AsKeyboardHandler   {

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
        this(Colors.DEFAULT_COLOR);
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

    /** ticked or not (null: not ticked) */
    @Override
    public void setValue(Boolean value) {
        this.value = value != null && value;
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
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }
    
    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        this.textRenderer.setText(buildRenderText());
        mySegments.clearAndMerge(this.textRenderer.getSegmentsBuffer().getSegments());
    }

    protected void init() {
        this.textRenderer = new TextComponent(this.getColor(), this.getRectangle(), buildRenderText());
        this.textRenderer.setParentContainer(this);
    }
    

    protected void checkBoxPressed(EventBus eventBus) {
        boolean newValue = !  CheckBoxInput.this.getValue();  // toggle the value
        CheckBoxInput.this.setValue(  newValue  );  
        eventBus.raiseEvent(new HasValue.ValueChangedEvent<>(CheckBoxInput.this) );
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
    }

    public void onClick(EventBus eventBus) {
        focus.requestFocus(eventBus);   // the mouse handling should have focused it already - just in case
        checkBoxPressed(eventBus);
    }

    private final OnChangeListeners<Boolean> changeListeners = new OnChangeListeners<Boolean>(this);   // onChange: any number of listeners
    public Registration onChange(EventHandler handler) {
        return changeListeners.add(handler);
    }

    /**
     * Focus: hilited, scrolled into view, the cursor on the box. ENTER or SPACE toggles it. Its own
     * "value changed" event goes to the onChange listener.
     */
    private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this)
            .showsCursorAt(new Position.Simple(0, 2))
            .handleKeys(AsKeyboardHandler.UTIL.toKeyboardHandler(
                    new KeyInputEventHandler((te) -> checkBoxPressed(te.getEventBus()), Keys.ENTER_KEYSTROKE, Keys.SPACE_KEYSTROKE)))
            .handleEvents(new ConditionalEventHandler.Impl(
                    (te) -> changeListeners.canHandle(te) != null,
                    (te) -> changeListeners.handle(te)));

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.textRenderer);
    }

}
