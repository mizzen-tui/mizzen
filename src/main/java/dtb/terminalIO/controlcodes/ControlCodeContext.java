package dtb.terminalIO.controlcodes;

import dtb.terminalIO.controlcodes.ControlCode.BaseTextControlCode;

/**
 * Represents the context where a ContolCode (or more generally a Renderer)
 * will output its contents to via the {@link #write(String)} method.
 * 
 * @author david
 *
 */
public interface ControlCodeContext {

    boolean isControlCodesEnabled();

    void setControlCodesEnabled(boolean enable);

    boolean isColorEnabled();

    void write(String s);

    default void write(BaseTextControlCode cc) {
        write(cc.getControlCode());
    }
    
    boolean  isRaw();
    
    boolean isCooked();

    public static abstract class AbstractControlCodeContext implements ControlCodeContext {

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

    
    /**
     * A {@link ControlCodeContext} that outputs to a Terminal
     */
    public class TerminalContextDelegate implements ControlCodeContext {

        ControlCodeContext delegate;

        public TerminalContextDelegate(ControlCodeContext delegate) {
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

        @Override
        public void write(BaseTextControlCode cc) {
            this.delegate.write(cc);
        }

	@Override
	public boolean isRaw() {
	    return this.delegate.isRaw();
	}

	@Override
	public boolean isCooked() {
	    return this.delegate.isCooked();
	}

    }

    /**
     * A {@link ControlCodeContext} that outputs to a String
     * 
     * <p/>
     * 
     * Useful when you wish to just get the resulting string
     * output of a renderer (or renderers).
     * 
     */
    public static class StringContext extends AbstractControlCodeContext {

        public StringBuffer sb;
        public boolean isRAW = false;

        public StringContext() {
            sb = new StringBuffer();
            isRAW=false;
        }

        public StringContext(boolean isRAW) {
            sb = new StringBuffer();
            this.isRAW=isRAW;
        }

        @Override
        public void write(String s) {
            sb.append(s);
        }
        
        public String toString() {
            return sb.toString();
        }
        
        public void clear() {
            sb = new StringBuffer();
        }

	@Override
	public boolean isRaw() {
	    return this.isRAW;
	}

	@Override
	public boolean isCooked() {
	    return !this.isRAW;
	}
    };

    public static ControlCodeContext DEFAULT = new AbstractControlCodeContext() {
        @Override
        public void write(String s) {
            System.out.println(s);
        }

	@Override
	public boolean isRaw() {
	    return false;
	}

	@Override
	public boolean isCooked() {
	    return true;
	}

    };

}
