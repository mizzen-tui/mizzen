package mizzen.components.scroll;

import java.util.function.Predicate;

import mizzen.components.HasName;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.TerminalEvent;
import mizzen.events.TerminalEvent.IS_Event_TYPE;
import mizzen.geom.Rectangle;

public interface HasViewPortWindow {

    Rectangle getViewPortAsRectangle();

    public static class Simple implements HasViewPortWindow {

        private Rectangle rectangle;

        public Simple(Rectangle rectangle) {
            super();
            this.rectangle = rectangle;
        }

        @Override
        public Rectangle getViewPortAsRectangle() {
            return rectangle;
        }

    }

    public static interface OnChange {
        void onViewPortWindowChange(HasViewPortWindow source, TerminalEvent<?> te);
    }

    public static class ViewPortWindowChangedEvent extends TerminalEvent<HasViewPortWindow> {

        private static final long serialVersionUID = 4544682502113778402L;

        public ViewPortWindowChangedEvent(HasViewPortWindow eventData) {
            super(eventData, TYPE.VIEW_OFFSET_CHANGED, eventData);
        }

        public String toString() {
            return "ViewOffsetChanged.ViewOffsetChangedEvent [source=" + getSource() + ", eventData="
                    + this.getEventData() + "]";
        }
    }

    abstract public class OnViewPortWindowChangedEventHandler implements ConditionalEventHandler, HasName<OnViewPortWindowChangedEventHandler> {

        static Predicate<TerminalEvent<?>> offsetChangedPredicate = new IS_Event_TYPE(TerminalEvent.TYPE.VIEW_OFFSET_CHANGED);

        HasViewPortWindow targetSource;
        String name;

        public OnViewPortWindowChangedEventHandler(HasViewPortWindow targetSource) {
            this.targetSource = targetSource;
        }

        @SuppressWarnings("unchecked")
        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (!offsetChangedPredicate.test(te)) {
                return null; // null means - can't handle.
            }
            HasViewPortWindow source = (HasViewPortWindow) te.getEventData();
            if (!source.equals(targetSource)) {
                return null;
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handle(TerminalEvent<?> te) {
            offsetChanged(te, (HasViewPortWindow) te.getEventData());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public OnViewPortWindowChangedEventHandler setName(String name) {
            this.name = name;
            return this;
        }

        abstract public void offsetChanged(TerminalEvent<?> te, HasViewPortWindow eventData);

    }

    public static class UTIL {

        public static OnViewPortWindowChangedEventHandler wrapHandler(HasViewPortWindow container, EventHandler consumeChange) {

            if (consumeChange == null) {
                return null;
            }

            // build new handler
            OnViewPortWindowChangedEventHandler newOnChangedHandler = new OnViewPortWindowChangedEventHandler(container) {
                @Override
                public void offsetChanged(TerminalEvent<?> te, HasViewPortWindow eventData) {
                    consumeChange.handle(te);
                }
            };

            HasName.UTIL.decorateWithParentsNamePrefix(newOnChangedHandler, container);

            return newOnChangedHandler;
        }
        
        
        public static OnViewPortWindowChangedEventHandler bindConsumerAsOnViewPortWindowChangedEventHandler(HasViewPortWindow container, 
                                                                                                            OnViewPortWindowChangedEventHandler previousHandler,
                                                                                                            EventHandlerContainer eventHandlerContainer,
                                                                                                            EventHandler consumeChange) {

            // remove old
            if (previousHandler != null) {
                eventHandlerContainer.removeEventHandler(previousHandler);
            }

            if (consumeChange == null) {
                return null;
            }

            // build new handler
            OnViewPortWindowChangedEventHandler newOnChangedHandler = wrapHandler(container, consumeChange);
            
            if (newOnChangedHandler == null) {
                return null;
            }

            // attach new
            eventHandlerContainer.appendEventHandler(newOnChangedHandler);
            return newOnChangedHandler;
        }

    }

}
