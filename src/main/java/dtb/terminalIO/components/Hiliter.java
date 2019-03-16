package dtb.terminalIO.components;

public interface Hiliter {

    void hilite();
    void unHilite();
    
    public static final Hiliter NOOP = new Hiliter() {
            @Override
            public void hilite() {
            }

            @Override
            public void unHilite() {
            }
    };

}
