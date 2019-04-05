package dtb.terminalIO.components.input;

import java.util.function.Predicate;

import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;

/**
 * A container that has a value.   It just allows for an extra level of indirection.
 *
 * @param <V>
 */
public interface HasValue<V> {
    
    V getValue();

    public static class ValueChangedEvent<V> extends  TerminalEvent<HasValue<V>> {

        private static final long serialVersionUID = 4544682502113778402L;

        public ValueChangedEvent(HasValue<V> eventData) {
            super(eventData, TYPE.VALUE_CHANGED, eventData);
        }
    }

    
    public abstract class OnValueChangedEventHandler<V>  implements ConditionalEventHandler {

        static Predicate<TerminalEvent<?>> valueChangedPredicate = new IS_Event_TYPE(TerminalEvent.TYPE.VALUE_CHANGED);

        HasValue<V>  targetSource;
        
        Object[] valuesOfInterest;
        
        public OnValueChangedEventHandler(HasValue<V>  targetSource, V[] valuesOfInterest) {
            this.targetSource = targetSource;
            this.valuesOfInterest = valuesOfInterest;
        }
        
        public OnValueChangedEventHandler(HasValue<V>  targetSource, V valueOfInterest) {
            this.targetSource = targetSource;
            this.valuesOfInterest = new Object[] {valueOfInterest};
        }
        
        public OnValueChangedEventHandler(HasValue<V>  targetSource) {
            this.targetSource = targetSource;
            this.valuesOfInterest = null;
        }
        
        
        @SuppressWarnings("unchecked")
        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if ( ! valueChangedPredicate.test(te) ) {
                return null ; //  null means - can't handle.
            }
            
            HasValue<V> source = (HasValue<V>)te.getEventData();
            
            if ( ! source.equals(targetSource)) {
                return null;
            }
            
            if (valuesOfInterest == null || valuesOfInterest.length == 0) {
                return this; //  listener doesn't care about value - usual case
            }
            
            V value = source.getValue();
            
            for (int i=0 ; i < valuesOfInterest.length; i++) {
                if (valuesOfInterest[i] != null && valuesOfInterest[i].equals(value)) {
                    return this;  
                }
            }
            
            return null;   // no match!
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void handle(TerminalEvent<?> te) {
            valueChanged((HasValue<V>)te.getEventData());
        }

        abstract public void valueChanged(HasValue<V> source);
        
        
    }
    
    
}
