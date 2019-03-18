package dtb.terminalIO.events;

import java.util.Comparator;

import dtb.terminalIO.geom.Position;

public interface Focusable extends EventHandler {


    boolean hasFocus();

    void setFocus(boolean focus);

    Position getPosition();
    
    
    class FocusableDelegate implements Focusable {
        
        Focusable delegate ;
        public FocusableDelegate(Focusable delegate) {
            super();
            this.delegate = delegate;
        }
    
        @Override
        public void handle(TerminalEvent<?> te) {
            delegate.handle(te);
        }
    
        @Override
        public boolean hasFocus() {
            return delegate.hasFocus();
        }
    
        @Override
        public void setFocus(boolean focus) {
            delegate.setFocus(focus);
        }
    
        @Override
        public Position getPosition() {
            return delegate.getPosition();
        }
        
    }


    class FocusableComparator implements Comparator<Focusable> {
        @Override
        public int compare(Focusable f1, Focusable f2) {
            if (f1 == f2)
                return 0;
            return Position.UTIL.POS_COMPARE.compare(f1.getPosition(), f2.getPosition());
        }
    }
    
    
}