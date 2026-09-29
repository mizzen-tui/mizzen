package dtb.terminalIO.components;

@FunctionalInterface
public interface Enabled {
    boolean isEnabled();
    
    public static Enabled ALWAYS_ENABLED = ()-> true;
    public static Enabled ALWAYS_DISABLED = ()-> false;
}
