package dtb.terminalIO.components;

import dtb.terminalIO.geom.Dimension;

/**
 * Specifies how to resize itself
 */
public interface CanResize {

    
    /**
     * resize object with the current size
     */
    public void resize(Dimension d);
    
}
