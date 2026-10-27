package mizzen.controlcodes;

import mizzen.components.HasName;
import mizzen.controlcodes.color.Colors;
import mizzen.util.linuxproc.TerminalStreamOutputParser;
import mizzen.util.linuxproc.TerminalStreamOutputParser.TerminalStreamConsumer;
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
    
    
    /** what the parser passes on: text as is, control codes as {NAME} */
    private static String parsed(String input) {
        StringBuilder out = new StringBuilder();
        new TerminalStreamOutputParser(new TerminalStreamConsumer() {
            @Override
            public void consumeTextChar(char ch) {
                out.append(ch);
            }

            @Override
            public void consumeControlCode(ControlCode controlCode) {
                out.append(controlCode instanceof mizzen.controlcodes.color.Color ? "{colour}" : "{" + controlCode + "}");
            }
        }).parseInput(input);
        return out.toString();
    }

    /** control codes it doesn't use are dropped, not shown as text (before: "[3J", a stray "B", "0;title" ...) */
    public void testUnusedControlCodesAreNotShown() {
        assertEquals("AB", parsed("A\033[3JB"));             // "clear" ends with this (clear scrollback)
        assertEquals("AB", parsed("A\033[?2004hB"));         // bash: bracketed paste
        assertEquals("AB", parsed("A\033[5;10HB"));          // cursor position
        assertEquals("AB", parsed("A\033]0;title\007B"));    // window title
        assertEquals("AB", parsed("A\033]0;title\033\\B"));  // ... ended by ESC \\ instead
        assertEquals("AB", parsed("A\0337\0338B"));          // save / restore cursor
        assertTrue(parsed("A\033(B\033[mB").matches("A(\\{colour\\})?B"));   // tput sgr0
    }

    public void testCursorMovesWithoutACount() {
        assertEquals("A{CursorUp(1)}B", parsed("A\033[AB"));
    }

    /** ESC[nG: to column n */
    public void testCursorToColumn() {
        assertEquals("A{" + ControlCode.CURSOR_LEFT + "}B", parsed("A\033[GB"));
        assertTrue(parsed("A\033[10GB").startsWith("A{" + ControlCode.CURSOR_LEFT + "}{"));
    }

    public void testTextAroundCodesIsKept() {
        assertEquals("ç ⠁ █", parsed("ç ⠁ █"));
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
