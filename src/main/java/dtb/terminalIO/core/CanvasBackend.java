package dtb.terminalIO.core;

import dtb.terminalIO.geom.Position;
import dtb.terminalIO.textsegments.ScreenLineSegment;

/**
 * Encapsulates where a {@link Canvas} object outputs its contents to when
 * the {@link Canvas#refresh()} method is called.
 * 
 * @author david
 *
 */
public interface CanvasBackend {

    void writeToBackend(ScreenLineSegment sc);

    void signal_eol();

    void refresh();

    void cr();

    void showCursorAt(Position showCursorPosition);

    void hideCursor();

    void showCursor();
    
    
    public static class NullCanvasBackEnd implements CanvasBackend {
        
        @Override
        public void writeToBackend(ScreenLineSegment sc) {
        }
    
        @Override
        public void signal_eol() {
        }
    
        @Override
        public void refresh() {
        }
    
        @Override
        public void cr() {
        }
    
        @Override
        public void showCursorAt(Position showCursorPosition) {
            // TODO Auto-generated method stub
            
        }
    
        @Override
        public void hideCursor() {
            // TODO Auto-generated method stub
            
        }
    
        @Override
        public void showCursor() {
            // TODO Auto-generated method stub
            
        }
    }
    
    
}