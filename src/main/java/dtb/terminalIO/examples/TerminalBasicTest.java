package dtb.terminalIO.examples;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.Renderer;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.ColorBGPipe;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.util.Stty;

public class TerminalBasicTest {

    public static void  main(String args[]) throws Exception {
        
        SyncToScreen terminalWriterSynch = makeTerminalSynch(args);

//        test_CRs(terminalWriterSynch);
//        test_BASIC_FOREGROUND_COLOR(terminalWriterSynch);
//        test_BASIC_BACKGROUND_COLOR_USING_RCOL_TO_RESET(terminalWriterSynch);
//        test_BASIC_BACKGROUND_COLOR_USING_BackGroundColorRESET_TO_RESET(terminalWriterSynch);
//        test_BASIC_BACKGROUND_COLOR_USING_BackGroundColorRESET_TO_RESET_PRESERVE_FOREGROUND(terminalWriterSynch);
//        test_BASIC_BACKGROUND_COLOR_USING_ColorPipe(terminalWriterSynch);
        test_BASIC_BACKGROUND_COLOR_USING_ColorPipe_withCURSOR_MOVEMENT(terminalWriterSynch);
        
        terminalWriterSynch.refresh();
        
        Stty.setCooked();
        

    }

    private static void test_CRs(SyncToScreen terminalWriterSynch) {
        terminalWriterSynch.write("TEST CR's  ");
        terminalWriterSynch.write(ControlCode.CR);

        terminalWriterSynch.write("Line1: Hello there! "+terminalWriterSynch.getClass().getSimpleName()+" ");

        terminalWriterSynch.write(ControlCode.CR);
        
        terminalWriterSynch.write("Line2: This should be on line 2. If it is not, then failed test");
        
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write(ControlCode.CR);
    }

    private static void test_BASIC_FOREGROUND_COLOR(SyncToScreen terminalWriterSynch) {
        terminalWriterSynch.write("TEST BASIC FOREGROUND COLOR  ");
        terminalWriterSynch.write(ControlCode.CR);

        terminalWriterSynch.write(Colors.IRed);
        terminalWriterSynch.write("Hopefully this shows up as a bright RED foreground color");
        
        terminalWriterSynch.write(Colors.RCol);
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("Hopefully this has no color");

        
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write(ControlCode.CR);
    }
    
    private static void test_BASIC_BACKGROUND_COLOR_USING_RCOL_TO_RESET(SyncToScreen terminalWriterSynch) {
        terminalWriterSynch.write(Colors.RCol);
        terminalWriterSynch.write("TEST BASIC BACKGROUND COLOR USING RCOL to RESET");
        terminalWriterSynch.write(ControlCode.CR);

        terminalWriterSynch.write(Colors.On_Blu);
        terminalWriterSynch.write("Hopefully this shows up as a BLUE background color");
        
        terminalWriterSynch.write(Colors.RCol);
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("Hopefully this has no color");

        
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write(ControlCode.CR);
    }
    
    private static void test_BASIC_BACKGROUND_COLOR_USING_BackGroundColorRESET_TO_RESET(SyncToScreen terminalWriterSynch) {
        terminalWriterSynch.write(Colors.RCol);
        terminalWriterSynch.write("TEST BASIC BACKGROUND COLOR USING BackGroundColorRESET to RESET");
        terminalWriterSynch.write(ControlCode.CR);

        terminalWriterSynch.write(Colors.On_Blu);
        terminalWriterSynch.write("Hopefully this shows up as a BLUE background color");
        
        terminalWriterSynch.write(Colors.BackGroundColorRESET);
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("Hopefully this has no color");

        
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write(ControlCode.CR);
    }

    private static void test_BASIC_BACKGROUND_COLOR_USING_BackGroundColorRESET_TO_RESET_PRESERVE_FOREGROUND(SyncToScreen terminalWriterSynch) {
        terminalWriterSynch.write(Colors.RCol);
        terminalWriterSynch.write("TEST BASIC BACKGROUND COLOR USING BackGroundColorRESET to RESET and PRESERVE FOREGOUND");
        terminalWriterSynch.write(ControlCode.CR);

        terminalWriterSynch.write(new FGBGColors(Colors.IRed, Colors.On_Blu));
        terminalWriterSynch.write("Hopefully this shows up as a Red Foreground BLUE background color");
        
        terminalWriterSynch.write(Colors.BackGroundColorRESET);
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("Hopefully this shows as Red with no background color");
        terminalWriterSynch.write(Colors.RCol);
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("Hopefully this has no color");

        
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write(ControlCode.CR);
    }

    private static void test_BASIC_BACKGROUND_COLOR_USING_ColorPipe(SyncToScreen terminalWriterSynch) {
        
        terminalWriterSynch = new SyncToScreenWithPipe(terminalWriterSynch);
        
        terminalWriterSynch.write(Colors.RCol);
        terminalWriterSynch.write("test_BASIC_BACKGROUND_COLOR_USING_ColorPipe_withCURSOR_MOVEMENT");
        terminalWriterSynch.write(ControlCode.CR);

        terminalWriterSynch.write( new FGBGColors(Colors.IRed, Colors.On_Blu));
        terminalWriterSynch.write("Hopefully this shows up as a Red Foreground BLUE background color");
        
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write( Colors.IRed);
        terminalWriterSynch.write("Hopefully this shows as Red with no background color");
        terminalWriterSynch.write(Colors.RCol);
        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("Hopefully this has no color");

        terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write(ControlCode.CR);
    }
    
    private static void test_BASIC_BACKGROUND_COLOR_USING_ColorPipe_withCURSOR_MOVEMENT(SyncToScreen terminalWriterSynch) {
        
        
        terminalWriterSynch = new SyncToScreenWithPipe(terminalWriterSynch);
        
        //ColorBGPipe.DISABLE = true;
        
        terminalWriterSynch.write(Colors.RCol);
        terminalWriterSynch.write("test_BASIC_BACKGROUND_COLOR_USING_ColorPipe_withCURSOR_MOVEMENT");
        terminalWriterSynch.write(ControlCode.CR);

        Color fgbgClr = new FGBGColors(Colors.IRed, Colors.On_Blu);
        
        terminalWriterSynch.write( fgbgClr );
        terminalWriterSynch.write("+-------------------------------------------------------------------+"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("|                                                                   |"); terminalWriterSynch.write(ControlCode.CR);
        terminalWriterSynch.write("+-------------------------------------------------------------------+"); 
        
        terminalWriterSynch.write(new ControlCode.CursorBackward(30));
        terminalWriterSynch.write(new ControlCode.CursorUp(10));
        terminalWriterSynch.write( fgbgClr );
        terminalWriterSynch.write("MIDDLE? Ish?");
        terminalWriterSynch.write(new ControlCode.CursorDown(1));
        terminalWriterSynch.write(ControlCode.CURSOR_LEFT);
        terminalWriterSynch.write(new ControlCode.CursorForward(5));
        terminalWriterSynch.write( fgbgClr );
        terminalWriterSynch.write("Next line with still have background color?");

        terminalWriterSynch.write(new ControlCode.CursorDown(10));
        terminalWriterSynch.write(ControlCode.CR);
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

    static class  SyncToScreenWithPipe  implements SyncToScreen {
        SyncToScreen delegate;
        ColorBGPipe pipe = new ColorBGPipe();
        
        public SyncToScreenWithPipe(SyncToScreen delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(String s) {
            delegate.write(s);
        }

        @Override
        public void write(Renderer r) {
            delegate.write( pipe.wrap(r));
        }

        @Override
        public void refresh() {
            delegate.refresh();
        }
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
