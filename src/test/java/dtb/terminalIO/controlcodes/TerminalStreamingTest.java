package dtb.terminalIO.controlcodes;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.util.linuxproc.TerminalStreamOutputParser;
import dtb.terminalIO.util.linuxproc.TerminalStreamOutputParser.TerminalStreamConsumer;
import junit.framework.TestCase;

public class TerminalStreamingTest extends TestCase {

    
    public void testSimpleColor() {
        String colorTest = Colors.coloredText(Colors.BGre,"Loading image ..  Size: ");
        new TerminalStreamOutputParser(new TestStreamConsumer()).parseInput(colorTest.getBytes(), colorTest.getBytes().length);
    }
    
    public void testSimpleContolCode() {
        String codeTest = "Hello there " + ControlCode.ERASE_END_OF_LINE.asString() +" what is up?";
        new TerminalStreamOutputParser(new TestStreamConsumer()).parseInput(codeTest);
    }
    
    public void testSimpleContolCode2() {
        String codeTest = Colors.BBlu.asString() + "1Another"+ControlCode.ERASE_END_OF_LINE.asString()+" test " + ControlCode.ERASE_END_OF_LINE.asString()+ ControlCode.ERASE_SCREEN.asString() +" what is up?";
        new TerminalStreamOutputParser(new TestStreamConsumer()).parseInput(codeTest);
    }
    
    
    
    public void testSimpleContolCodeCursorUp() {
        String codeTest = "Another cursorUP "+new ControlCode.CursorUp(2).asString()+" test " ;
        new TerminalStreamOutputParser(new TestStreamConsumer()).parseInput(codeTest);
    }
    
    public void testSimpleContolCodeCursorDown() {
        String codeTest = "Another cursorDown "+new ControlCode.CursorDown(2).asString()+" test " ;
        new TerminalStreamOutputParser(new TestStreamConsumer()).parseInput(codeTest);
    }
    
    public void testSimpleContolCodeCursorForward() {
        String codeTest = "Another cursorForward "+new ControlCode.CursorForward(2).asString()+" test " ;
        new TerminalStreamOutputParser(new TestStreamConsumer()).parseInput(codeTest);
    }
    
    public void testSimpleContolCodeCursorBackward() {
        String codeTest = "Another cursorBackward "+new ControlCode.CursorBackward(2).asString()+" test " ;
        new TerminalStreamOutputParser(new TestStreamConsumer()).parseInput(codeTest);
    }
    
    
    static class TestStreamConsumer implements TerminalStreamConsumer {

        @Override
        public void consumeTextChar(char ch) {
            System.out.println(ch);
            
        }

        @Override
        public void consumeControlCode(ControlCode controlCode) {
            System.out.println("ctrl-code " + HasName.UTIL.getName(controlCode));
            
        }
        
    }
    
}
