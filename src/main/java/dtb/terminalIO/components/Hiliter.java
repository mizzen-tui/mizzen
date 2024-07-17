package dtb.terminalIO.components;

public interface Hiliter {

    void hilite();
    void unHilite();
    
    /***
     * A Hiliter that does nothing
     */
    public static final Hiliter NOOP = new Hiliter() {
            @Override
            public void hilite() {
            }

            @Override
            public void unHilite() {
            }
    };
    
    /**
     * Chain hilite actions 
     */
    public static class Chained implements Hiliter  {

        private Hiliter h1;
        private Hiliter h2;
        
        public Chained(Hiliter h1, Hiliter h2) {
            super();
            this.h1 = h1;
            this.h2 = h2;
        }

        @Override
        public void hilite() {
            h1.hilite();
            h2.hilite();
        }

        @Override
        public void unHilite() {
            h2.unHilite();
            h1.unHilite();
        }
        
    }

}
