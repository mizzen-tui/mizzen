package dtb.terminalIO.components.input;

import dtb.terminalIO.components.ColorHiliter;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.FocusSetterToHiliteAction;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.Focusable;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Position;

abstract class AsFocusable  implements Focusable {
     
    Component<?> component;
    
    Position position;
    
    EventBus eventBus;
    
    boolean hasFocus = false;
    
    ColorHiliter colorHiliter;
    
    AsFocusable(Component<?> component, Position position, EventBus eventBus) {
        this.component = component;
        this.position = position;
        this.eventBus = eventBus;
        colorHiliter =  new ColorHiliter(this.component.getColor());
    }
    
    @Override
    public boolean hasFocus() {
        return hasFocus;
    }

    @Override
    public void setFocus(boolean focus) {
        this.hasFocus = focus;
        new FocusSetterToHiliteAction().hiliteAction(this, colorHiliter);
        this.component.setColor( colorHiliter.asColor() );
    }

    @Override
    public Position getPosition() {
        return position;
    }


    @Override
    abstract public void handle(TerminalEvent<?> te);

}
