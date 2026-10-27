package mizzen.components.input;

import mizzen.components.Component;
import mizzen.events.EventBus;
import mizzen.events.RequestScrollToView;
import mizzen.geom.Rectangle;

public class ShowComponentOnFocus {

    public void onFocus(Component<?> componentWithFocus, EventBus eventBus) {

        eventBus.raiseEvent( new RequestScrollToView( componentWithFocus , componentWithFocus)  );

        // TODO:  maybe have the component "bringToFront" ?

    }

    public void onFocus(Component<?> componentWithFocus, Rectangle offsetRectangle, EventBus eventBus) {
 
        eventBus.raiseEvent( new RequestScrollToView( componentWithFocus , componentWithFocus, offsetRectangle)  );

    }

}
