package dtb.terminalIO.components.input;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestScrollToView;

public class ShowComponentOnFocus {
    
    public void onFocus(Component<?> componentWithFocus, EventBus eventBus) {
        
        eventBus.raiseEvent( new RequestScrollToView( componentWithFocus , componentWithFocus)  );
        
        // TODO:  maybe have the component "bringToFront"
        
    }

}
