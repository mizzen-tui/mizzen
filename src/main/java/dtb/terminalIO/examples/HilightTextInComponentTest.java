package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Collection;

import dtb.terminalIO.components.ComponentWithHilightedText;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.SegmentsSearchUtils;
import dtb.terminalIO.util.StringUtils;

public class HilightTextInComponentTest {

    
    public static void main(String[] args) throws IOException {

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);
        
        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        
        ComponentWithHilightedText container = new ComponentWithHilightedText();
        
        container.setBounds(new Dimension.Simple(20,50));
        
        
        TextComponent tc = new TextComponent( StringUtils.wrap("It was July 21, 1969, and Neil Armstrong awoke with a start. "
                + "It was the day he would become the first human being to ever walk on the moon. "
                + "The journey had begun several days earlier, when on July 16th, the Apollo 11 "
                + "launched from Earth headed into outer space. On board with Neil Armstrong were "
                + "Michael Collins and Buzz Aldrin. The crew landed on the moon in the Sea of Tranquility "
                + "a day before the actual walk. Upon Neil’s first step onto the moon’s surface, he declared, "
                + "“That’s one small step for man, one giant leap for mankind.” It sure was!",
                                                                40));

        container.add(1,1,tc);
        
        String srchSTR = "Neil";
        
        Collection<Rectangle> result = SegmentsSearchUtils.searchTextInSegmentsAll(container.getSegmentsBuffer(),srchSTR);
        
        //List<Component> hilightList = new ArrayList<Component>();
        
        for (Rectangle r : result) {
            container.addHilitedRegion(r);
        }
        
        master_canvas.addComponentSegments(1,1,container);
        
        master_canvas.refresh();
        
        TestsCommon.sleep(1000);
        
        for (Rectangle r : result) {
            TestsCommon.sleep(1000);
            container.clearHiliteRegion(r);
            master_canvas.addComponentSegments(1,1,container);
            master_canvas.t(master_canvas.getHeight()-3, 1, String.valueOf(r)+ "     ");
            master_canvas.refresh();
        }
        
        
        
    }
    
    
}
