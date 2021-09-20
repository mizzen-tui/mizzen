package dtb.terminalIO.components;

import java.util.function.Consumer;

import dtb.terminalIO.geom.Dimension;

/**
 * Specifies how to resize itself
 */
public interface CanResize {

    
    /**
     * resize object with the current size
     */
    public void resize(Dimension d);
    
    public static class OnResizeService implements CanResize {

        Consumer<Dimension> onResize = null; 

        @Override
        public void resize(Dimension size) {
            if (onResize != null) {
                onResize.accept(size);
            }
        }
        
        public void setOnResize(Consumer<Dimension> onResize) {
            this.onResize = onResize;
        }
    }
    
}
