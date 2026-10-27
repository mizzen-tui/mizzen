package mizzen.components.scroll;

public interface ViewPortNavigable {

    public void up(int iLines);

    public void down(int iLines) ;

    public void left(int iCols);

    public void right(int iCols);

    public static class NOOP implements ViewPortNavigable {

        @Override
        public void up(int iLines) {
        }

        @Override
        public void down(int iLines) {
        }

        @Override
        public void left(int iCols) {
        }

        @Override
        public void right(int iCols) {
        }

    }
    
    public static class Delegate implements ViewPortNavigable {
        
        ViewPortNavigable delegate;
        
        public Delegate(ViewPortNavigable delegate) {
            this.delegate = delegate;
        }
        
        protected ViewPortNavigable getDelegate() {
            return delegate;
        }

        @Override
        public void up(int iLines) {
             getDelegate().up(iLines);
        }

        @Override
        public void down(int iLines) {
            getDelegate().down(iLines);
        }

        @Override
        public void left(int iCols) {
            getDelegate().left(iCols);
        }

        @Override
        public void right(int iCols) {
            getDelegate().right(iCols);
        }

        
        
    }
    
    
}
