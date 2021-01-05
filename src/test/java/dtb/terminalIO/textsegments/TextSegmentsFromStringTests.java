package dtb.terminalIO.textsegments;

import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.textsegments.ScreenLineSegment.UTIL.SegmentsFromStringResult;
import junit.framework.TestCase;

public class TextSegmentsFromStringTests extends TestCase {

    
    private String someSizeText() {
        Dimension someSize = new Dimension.Simple(10, 10);
        return String.valueOf(someSize);
    }

    private String problemColoredStr1() {
        String coloredTextThePrevioislyCauseAProblem = Colors.coloredText(Colors.BGre,"Loading image ..  Size: ") + 
                                                       Colors.coloredText(Colors.Yel, someSizeText());
        return coloredTextThePrevioislyCauseAProblem;
    }


    public void testProblemStr1OnColor() {
        
        SegmentsFromStringResult result = ScreenLineSegment.UTIL.textSegmentsFromStringWithEmbeddedColor(problemColoredStr1(), 0, 0, 
                                                                               new Rectangle.Simple(0,0,2,100),
                                                 (strColor, p, txt) -> new SegmentImpl( strColor != null ? strColor : Colors.DEFAULT_COLOR, p, -1, txt));

        assertEquals(result.size(), 3);

        
        
        
    }

    
    
}
