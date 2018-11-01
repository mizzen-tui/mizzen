package dtb.terminalIO;

import java.io.PrintStream;

public interface CLIEmitter {

	public void print(String s);

	public void println(String s);

	public static abstract class AbstractCLIEmitter implements CLIEmitter {

		abstract PrintStream getOutputStream();

		@Override
		public void print(String s) {
			this.getOutputStream().print(s);
		}

		@Override
		public void println(String s) {
			this.getOutputStream().println(s);
		}
	}

	public static class DelegateEmitter implements CLIEmitter {
		CLIEmitter delegate;

		public DelegateEmitter(CLIEmitter delegate) {
			this.delegate = delegate;
		}

		public void print(String s) {
			this.delegate.print(s);
		}

		public void println(String s) {
			this.delegate.print(s);
		}

	}

	public static class PrintStreamEmitter extends AbstractCLIEmitter {
		PrintStream out;

		public PrintStreamEmitter(PrintStream out) {
			this.out = out;
		}

		public PrintStreamEmitter() {
			this(System.out);
		}

		@Override
		PrintStream getOutputStream() {
			return out;
		}
	}

	public static CLIEmitter DEFAULT = new PrintStreamEmitter();
}