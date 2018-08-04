package dtb.terminalIO;

public interface TerminalContext {

    boolean isControlCodesEnabled();

    void setControlCodesEnabled(boolean enable);

    boolean isColorEnabled();

    void write(String s);
    
    default boolean  isRaw() {
        return false;
    }
    
    default boolean isCooked() {
        return true;
    }

    public static abstract class AbstractTerminalContext implements TerminalContext {

        boolean controlCodesEnabled = true;

        @Override
        public boolean isControlCodesEnabled() {
            return controlCodesEnabled;
        }

        @Override
        public void setControlCodesEnabled(boolean enable) {
            this.controlCodesEnabled = enable;
        }

        @Override
        public boolean isColorEnabled() {
            return isControlCodesEnabled(); // todo: add a new flag
        }
    };

    public class TerminalContextDelegate implements TerminalContext {

        TerminalContext delegate;

        public TerminalContextDelegate(TerminalContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isControlCodesEnabled() {
            return this.delegate.isControlCodesEnabled();
        }

        @Override
        public void setControlCodesEnabled(boolean enable) {
            this.delegate.setControlCodesEnabled(enable);
        }

        @Override
        public boolean isColorEnabled() {
            return this.delegate.isColorEnabled();
        }

        @Override
        public void write(String s) {
            this.delegate.write(s);
        }

    }

    public static class StringContext extends AbstractTerminalContext {

        public StringBuffer sb;

        public StringContext() {
            sb = new StringBuffer();
        }

        @Override
        public void write(String s) {
            sb.append(s);
        }
        
        public String toString() {
            return sb.toString();
        }

    };

    public static TerminalContext DEFAULT = new AbstractTerminalContext() {
        @Override
        public void write(String s) {
            System.out.println(s);
        }

    };

}
