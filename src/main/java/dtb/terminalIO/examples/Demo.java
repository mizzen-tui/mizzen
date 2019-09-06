package dtb.terminalIO.examples;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer.Provider;

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

        void addAndRememberEventHandler(EventHandlerContainer eventBus, ConditionalEventHandler eventHandler) {
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