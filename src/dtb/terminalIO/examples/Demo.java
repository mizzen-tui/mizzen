package dtb.terminalIO.examples;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.IScreenLineSegmentsBuffer.Provider;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;

public interface Demo {

    void addAllEventHandlers(EventBus eventBus);

    void renderPreUPdate();

    void flushOutput();

    void setUpDemo(EventBus eventBus, Rectangle bounds, IScreenLineSegmentsBuffer.Provider segmentsBufferProvider);

    void tearDownDemo(EventBus eventBus);

    void resize(Dimension size);
    
    
    public static abstract class SimpleDemo implements Demo {

        List<ConditionalEventHandler> eventHandlers = new ArrayList<ConditionalEventHandler>();

        void addAndRememberEventHandler(EventBus eventBus, ConditionalEventHandler eventHandler) {
            eventBus.addEventHandler(eventHandler);
            eventHandlers.add(eventHandler);
        }

        public void addAllEventHandlers(EventBus eventBus) {
            for (ConditionalEventHandler e : eventHandlers) {
                eventBus.addEventHandler(e);
            }
        }
        
        void removeAllEventHandlers(EventBus eventBus) {
            for (ConditionalEventHandler e : eventHandlers) {
                eventBus.remove(e);
            }
        }
        
        public void renderPreUPdate() {
            // do nothing
        }

        public void flushOutput() {
            // do nothing
        }


        @Override
        abstract public void setUpDemo(EventBus eventBus, Rectangle bounds, Provider segmentsBufferProvider);

        public void tearDownDemo(EventBus eventBus) {
            removeAllEventHandlers(eventBus);
        }
        
    }



}