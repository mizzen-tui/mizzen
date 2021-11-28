package dtb.terminalIO.traverse;

public abstract class HasParentAdapter<C, P> {
    
    abstract public P  getParent(C child);
    
}