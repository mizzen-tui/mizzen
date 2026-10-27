package mizzen.events.mouse;

import mizzen.events.EventBus;

@FunctionalInterface
public interface Clickable {

    public void onClick(EventBus eventBus);

    default public void onClick(ApplicationMouseEvent appMouseEvent) {
        if (MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())) {
            onClick( appMouseEvent.getEventBus());
        }
    }

    public static final Clickable NOOP_Clickable = new  Clickable() {
        @Override
        public void onClick(EventBus eventBus) {
            // do nothing
        }
    };

    public static class ChainedClickable implements  Clickable {

        private Clickable clickable1 = NOOP_Clickable;
        private Clickable clickable2 = NOOP_Clickable;
        
        public ChainedClickable(Clickable clickable1, Clickable clickable2) {
            super();
            appendOnClick(clickable1);
            appendOnClick(clickable2);
        }

        public ChainedClickable() {
            this(NOOP_Clickable, NOOP_Clickable);
        }
        

        public void onClick(ApplicationMouseEvent appMouseEvent) {
            if (clickable1 != null) {
                clickable1.onClick(appMouseEvent);
            }
            if (clickable2 != null) {
                clickable2.onClick(appMouseEvent);
            }
        }

        @Override
        public void onClick(EventBus eventBus) {
            if (clickable1 != null) {
                clickable1.onClick(eventBus);
            }
            if (clickable2 != null) {
                clickable2.onClick(eventBus);
            }
        }

        public ChainedClickable appendOnClick(Clickable newClickable) {
            if (
                newClickable == null           || 
                newClickable == NOOP_Clickable ||
                newClickable == clickable1 ||
                newClickable == clickable2 
               ) {
                return this;
            }

            if (clickable1 == NOOP_Clickable) {
                this.clickable1 = newClickable;
                return this;
            }

            if (clickable2 == NOOP_Clickable) {
                this.clickable2 = newClickable;
                return this;
            }

            this.clickable2 = new ChainedClickable(this.clickable2, newClickable);

            return this;
        }
        
        
        public static Clickable combineClickables(Clickable clickable1, Clickable clickable2) {
            if (clickable1 == null           || 
                clickable1 == NOOP_Clickable ||
                clickable1 == clickable2 ) {
                    return clickable2;
            }
            
            if (clickable2 == null           || 
                clickable2 == NOOP_Clickable ||
                clickable2 == clickable1 ) {
                        return clickable1;
            }
            
            return new ChainedClickable(clickable1, clickable2);
            
        }
        
    }
}
