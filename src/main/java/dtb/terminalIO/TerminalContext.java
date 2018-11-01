package dtb.terminalIO;

import dtb.terminalIO.ControlCode.BaseTextControlCode;

public interface TerminalContext {

    boolean isControlCodesEnabled();

    void setControlCodesEnabled(boolean enable);

    boolean isColorEnabled();

    void write(String s);

    default void write(BaseTextControlCode cc) {
	write(cc.getControlCode());
    }
    
    boolean  isRaw();
    
    boolean isCooked();

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

    public static class StringContext extends AbstractTerminalContext {

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

	@Override
	public boolean isRaw() {
	    return this.isRAW;
	}

	@Override
	public boolean isCooked() {
	    return !this.isRAW;
	}
    };

    public static TerminalContext DEFAULT = new AbstractTerminalContext() {
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
