package mizzen.components.input;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.controlcodes.color.Color;
import mizzen.events.EventBus;
import mizzen.events.mouse.Clickable;

public class FocusableLabel extends AbstractComponentWrappper<TextComponent> implements Clickable {

    Clickable clickable;

    public FocusableLabel(Color color, String text) {
        super(color, new TextComponent(color,text));
        clickable = (eventBus) -> {
            Component.UTIL.bringToFront(FocusableLabel.this);
        };
    }

    public Clickable getClickableAction() {
        return this.clickable;
    }

    public FocusableLabel setOnClickAction(Clickable clickable) {
        this.clickable = clickable;
        return this;
    }

    public FocusableLabel appendOnClickAction(Clickable newClickable) {
        return setOnClickAction( Clickable.ChainedClickable.combineClickables(this.clickable, newClickable) );
    }
    
    
    @Override
    public void onClick(EventBus eventBus) {
        this.clickable.onClick(eventBus);
    }
    

}
