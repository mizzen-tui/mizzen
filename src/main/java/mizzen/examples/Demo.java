package mizzen.examples;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.Component;
import mizzen.components.HasName;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;

public interface Demo {

    void addAllEventHandlers(EventHandlerContainer eventBus);

    void renderPreUPdate();

    void flushOutput();

    void setUpDemo(EventHandlerContainer eventBus, Rectangle drawingArea);

    void tearDownDemo(EventHandlerContainer eventBus);

    void resize(Dimension size);
    
    
    public static abstract class SimpleDemo implements Demo {

        List<ConditionalEventHandler> eventHandlers = new ArrayList<ConditionalEventHandler>();
        Runnable flushDemoOutput;

        /**
         * For a demo's timers and other non-key handlers, which come and go with the demo. Not for a
         * component: put on the event bus, it would get keys ahead of focus routing, wherever focus
         * is - a component gets its keys through the component tree.
         */
        void addAndRememberEventHandler(EventHandlerContainer eventBus, ConditionalEventHandler eventHandler) {
            if (eventHandler instanceof Component) {
                throw new IllegalArgumentException("addAndRememberEventHandler(" + HasName.UTIL.getName(eventHandler)
                        + "): a component gets its keys through the component tree - add it to a container instead");
            }
            eventBus.appendEventHandler(eventHandler);
            eventHandlers.add(eventHandler);
        }

        public void addAllEventHandlers(EventHandlerContainer eventBus) {
            for (ConditionalEventHandler e : eventHandlers) {
                eventBus.appendEventHandler(e);
            }
        }
        
        void removeAllEventHandlers(EventHandlerContainer eventBus) {
            for (ConditionalEventHandler e : eventHandlers) {
                eventBus.removeEventHandler(e);
            }
        }
        
        public void renderPreUPdate() {
            // do nothing
        }

        public void setFlushDemoOutput(Runnable flushDemoOutput) {
            this.flushDemoOutput = flushDemoOutput;
        }

        public void flushOutput() {
            if (flushDemoOutput != null) {
                flushDemoOutput.run();
            }
        }


        @Override
        abstract public void setUpDemo(EventHandlerContainer eventBus, Rectangle drawingArea);

        public void tearDownDemo(EventHandlerContainer eventBus) {
            removeAllEventHandlers(eventBus);
        }
        
    }



}