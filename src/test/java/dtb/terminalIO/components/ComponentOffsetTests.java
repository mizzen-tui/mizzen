package dtb.terminalIO.components;

import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.WindowViewComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.supportfortesting.CanvasForTesting;
import junit.framework.TestCase;

public class ComponentOffsetTests extends TestCase {
    
    
    private static String TC_TEXT = "The Rain\n in Spain\nStays mainly\non the plain!\nAnd now we are done\n";
    
    Canvas buildCanvasForTesting() throws Exception {
        return CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput();
    }
    

    public void testWindowViewComponent_getCanvasOffsets() throws Exception {

        System.out.println("testWindowViewComponent_getCanvasOffsets--");
        
        
        Canvas canvas = buildCanvasForTesting();

        TextComponent tc = new TextComponent(TC_TEXT);

        WindowViewComponent wc = new WindowViewComponent(Colors.DEFAULT_COLOR, new Dimension.Simple(10,10), tc);

        canvas.addComponentSegments(0, 0, wc);

        System.out.println( new Dimension.Simple(tc));

        System.out.println( Component.UTIL.getCanvasOffsetFor(tc));

        assertTrue(new Position.Simple(0,0).isSameLocation(Component.UTIL.getCanvasOffsetFor(tc)));

        wc.setViewOffset(new Position.Simple(5,5));

        System.out.println( Component.UTIL.getCanvasOffsetFor(tc));

        assertTrue(new Position.Simple(-5,-5).isSameLocation(Component.UTIL.getCanvasOffsetFor(tc)));


        Position canvasPosition = new Position.Simple(0,0);

        Position offsetInto_TC_from_canvas_00 = Component.UTIL.getCanvasOffsetFor(tc).offsetTo(canvasPosition);

        System.out.println(offsetInto_TC_from_canvas_00);
        
        assertTrue(new Position.Simple(5,5).isSameLocation(offsetInto_TC_from_canvas_00));


    }
    
    public void testScrollableComponent_getCanvasOffsets() throws Exception {
        
        System.out.println("testScrollableComponent_getCanvasOffsets--");
        
        Canvas canvas = buildCanvasForTesting();

        TextComponent tc = new TextComponent(TC_TEXT);

        ScrollableComponent sc = new ScrollableComponent(Colors.DEFAULT_COLOR, new Dimension.Simple(10,10), tc);

        canvas.addComponentSegments(0, 0, sc);

        System.out.println( new Dimension.Simple(tc));

        System.out.println( Component.UTIL.getCanvasOffsetFor(tc));

        assertTrue(new Position.Simple(1,1).isSameLocation(Component.UTIL.getCanvasOffsetFor(tc)));

        assertTrue(sc.setViewOffset(5,5));

        assertTrue(new Position.Simple(-4,-4).isSameLocation(Component.UTIL.getCanvasOffsetFor(tc)));

        System.out.println( Component.UTIL.getCanvasOffsetFor(tc));

        Position canvasPosition = new Position.Simple(1,1);

        Position offsetInto_TC_from_canvas_00 = Component.UTIL.getCanvasOffsetFor(tc).offsetTo(canvasPosition);

        System.out.println(offsetInto_TC_from_canvas_00);
        
        assertTrue(new Position.Simple(5,5).isSameLocation(offsetInto_TC_from_canvas_00));

    }

 

}
