package dtb.terminalIO.components;

/**
 * 
 * An object that can be 'dirty' ( considered modified )
 *
 */
public interface DirtyFlag {

    boolean isDirty();

    void markAsDirty();
    
    
}
