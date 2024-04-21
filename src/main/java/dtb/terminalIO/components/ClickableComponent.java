package dtb.terminalIO.components;

import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.mouse.Clickable;

public class ClickableComponent<T extends Component<?>> extends AbstractWrappedComponentAndFocusable<T> implements Clickable {

    Clickable clickable;
    Focusable focusable = null;

    public ClickableComponent(Color color, T container) {
        super(color, container);
        // specify default click behaviour
        clickable = (eventBus) -> {
            Component.UTIL.bringToFront(ClickableComponent.this);
        };
        focusable = new DefaultFocusableBehaviour.HiliteOnFocus(this.getWrappedComponent());
    }
    
    public ClickableComponent(T container) {
        this(container.getColor(), container);
    }

    
    public Clickable getClickableAction() {
        return this.clickable;
    }
    
    public void setOnClickAction(Clickable clickable) {
        this.clickable = clickable;
    }
    
    @Override
    public void onClick(EventBus eventBus) {
        this.clickable.onClick(eventBus);
    }

    @Override
    protected Focusable asFocusable() {
        return focusable;
    }


}
