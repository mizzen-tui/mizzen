package mizzen.core;

import java.io.PrintStream;

import mizzen.util.MapChars;

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

    public static class CountOutputEmitter extends DelegateEmitter {

        private int characterCount;

        public CountOutputEmitter(CLIEmitter baseEmitter) {
            super(baseEmitter);
            this.characterCount = 0;
        }

        public void print(String s) {
            characterCount += (s!=null) ? s.length() : 0;
            super.print(s);
        }

        public void println(String s) {
            characterCount += (s!=null) ? s.length()+1 : 0;  // +1 to account for implicit '\n'
            super.println(s);
        }
        
        public int getCharacterCount() {
            return characterCount;
        }

        @Override
        public String toString() {
            return "CountOutputEmitter  [characterCount=" + characterCount + "]";
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