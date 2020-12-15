package dtb.terminalIO.examples;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.Renderer;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.util.Stty;

public class TerminalBasicTest {

    public static void  main(String args[]) throws Exception {
        
        SyncToScreen terminalWriterSynch = makeTerminalSynch(args);
        
        terminalWriterSynch.write("Line1: Hello there! "+terminalWriterSynch.getClass().getSimpleName()+" ");

        terminalWriterSynch.write(ControlCode.CR);
        
        terminalWriterSynch.write("Line2: This should be on line 2. If it is not, then failed test");
        
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write(ControlCode.CR);

        terminalWriterSynch.refresh();
        
        Stty.setCooked();
        

    }
    
    static SyncToScreen makeTerminalSynch(String args[]) {
        if (args == null || args.length<1) {
            TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);
            TerminalSynch terminalWriterSynch = new TerminalSynch(screen) ;
            
            return terminalWriterSynch;
        }
        
        if (args[0].equalsIgnoreCase("SYSOUT")) {
            return new SysoutSyncToScreen();
        }
        
        return makeTerminalSynch(null);
        
    }
    
    interface SyncToScreen {
        void write(String s);
        void write(Renderer r);
        void refresh();
    }
    
    static class TerminalSynch implements SyncToScreen {

        TerminalScreen terminal;
        
        public TerminalSynch(TerminalScreen terminal) {
            this.terminal = terminal;
        }

        @Override
        public void write(String s) {
            terminal.write(s);
        }

        @Override
        public void write(Renderer r) {
            r.render(terminal);
        }

        @Override
        public void refresh() {
            terminal.refresh();
        }
        
    }
    
    static class SysoutSyncToScreen implements SyncToScreen {

        @Override
        public void write(String s) {
            System.out.print(s);
        }

        @Override
        public void write(Renderer r) {
            System.out.print(r.asString());
        }

        @Override
        public void refresh() {
            // do nothing
        }
        
    }
    
    
    static void testWithTerminal(String args[]) {
        TerminalScreen terminal = TestsCommon.getTerminalScreenForTesting(args);
        
        terminal.write("Line1: Hello there! " + terminal.isControlCodesEnabled());
        
        terminal.cr();
        
        terminal.write("Line2: This should be on line 2.");
        
        terminal.add( ControlCode.DISABLE_MOUSE_TRACKING );
        
        terminal.refresh();
        
    }
    
    static void testWithoutTerminal(String args[]) {
        
        System.out.print("Hello there!"+ControlCode.CR.asString());
        System.out.print("2nd line?");
        
    }

}
