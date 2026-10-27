package mizzen.core;

import mizzen.geom.position.Position;
import mizzen.textsegments.ScreenLineSegment;

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

    default void showCursorAt(Position showCursorPosition) {
        showCursorAt(showCursorPosition, false);
    }
    void showCursorAt(Position showCursorPosition, boolean showCursor);

    void hideCursor();

    void showCursor();
    
    void setSize(int height, int width);
    
    
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
        public void showCursorAt(Position showCursorPosition, boolean showCursor) {
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

        @Override
        public void setSize(int height, int width) {
            // TODO Auto-generated method stub
            
        }
    }


    
}