package mizzen.examples;

import java.util.Arrays;

import mizzen.components.BlockChars;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.controlcodes.color.Colors.FGBGColors;
import mizzen.core.Canvas;
import mizzen.core.ScreenCanvasBackend;
import mizzen.core.TerminalScreen;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

/**
 * Tests the block line drawing functions to ensure intersection characters are drawn where lines overlap
 * 
 * @author dbalme
 *
 */
public class CanvasDrawLinesTest {

    public static void  main(String args[]) throws Exception {
        TerminalScreen terminal = TestsCommon.getTerminalScreenForTesting(args);
        
        Canvas canvas = new Canvas(new ScreenCanvasBackend(terminal));

        canvas.setSize(24, 70);
        
        canvas.t(1,2,"Testing line drawing line chars: " + Arrays.toString( BlockChars.NICE.blockChars() ));
        timedRefresh(canvas);

        Color lineColor1 = new FGBGColors(Colors.BIYel, Colors.On_IBla);
        Color lineColor2 = new FGBGColors(Colors.BIGre, Colors.On_IBla);
        Color lineColor3 = new FGBGColors(Colors.BIRed, Colors.On_IBla);
        Color lineColor4 = new FGBGColors(Colors.BIBlu, Colors.On_IBla);
        Color lineColor5 = new FGBGColors(Colors.BICya, Colors.On_IBla);

        BlockChars.UTIL.drawHorizontalLine(canvas.getSegmentsBuffer(), lineColor1, BlockChars.NICE, new Position.Simple(10,10), 20);
        timedRefresh(canvas);

        BlockChars.UTIL.drawVerticalLine(canvas.getSegmentsBuffer(), lineColor1, BlockChars.NICE, new Position.Simple(5,15), 15);
        timedRefresh(canvas);


        BlockChars.UTIL.drawBox(canvas.getSegmentsBuffer(), lineColor1, BlockChars.NICE, new Rectangle.Simple(7, 12, 10, 10));
        timedRefresh(canvas);

        BlockChars.UTIL.drawBox(canvas.getSegmentsBuffer(), lineColor2, BlockChars.NICE, new Rectangle.Simple(10, 12, 10, 9));
        timedRefresh(canvas);

        BlockChars.UTIL.drawBox(canvas.getSegmentsBuffer(), lineColor3, BlockChars.NICE, new Rectangle.Simple(5, 5, 15, 25));
        timedRefresh(canvas);

        BlockChars.UTIL.drawHorizontalLine(canvas.getSegmentsBuffer(), lineColor4, BlockChars.NICE, new Position.Simple(13,3), 30);
        timedRefresh(canvas);

        BlockChars.UTIL.drawHorizontalLine(canvas.getSegmentsBuffer(), lineColor5, BlockChars.NICE, new Position.Simple(15,5), 25);
        timedRefresh(canvas);

    }
    
    static void timedRefresh(Canvas canvas ) throws InterruptedException {
        canvas.refresh();
        Thread.sleep(500);
    }
    
    
}
