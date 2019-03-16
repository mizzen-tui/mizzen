package dtb.terminalIO.core;

import java.io.PrintStream;

import dtb.terminalIO.util.MapChars;

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
        @Override
        public String toString() {
            return "DelegateEmitter [delegate=" + delegate + "]";
        }

        CLIEmitter delegate;

        public DelegateEmitter(CLIEmitter delegate) {
            this.delegate = delegate;
        }

        public CLIEmitter getDelegate() {
            return this.delegate;
        }

        public void setDelegate(CLIEmitter delegate) {
            this.delegate = delegate;
        }

        public void print(String s) {
            this.delegate.print(s);
        }

        public void println(String s) {
            this.delegate.print(s);
        }

    }

    public static class MapCharsEmitter extends DelegateEmitter {

        private MapChars mapChars;

        public MapCharsEmitter(CLIEmitter baseEmitter, MapChars mapChars) {
            super(baseEmitter);
            this.mapChars = mapChars;
        }

        public void print(String s) {
            super.print(mapChars.getMappedString(s));
        }

        public void println(String s) {
            super.println(mapChars.getMappedString(s));
        }

        @Override
        public String toString() {
            return "MapCharsEmitter [mapChars=" + mapChars + ", delegate=" + delegate + "]";
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

        @Override
        public String toString() {
            return "PrintStreamEmitter [out=" + out + "]";
        }
        
    }

    public static CLIEmitter DEFAULT = new PrintStreamEmitter();
}