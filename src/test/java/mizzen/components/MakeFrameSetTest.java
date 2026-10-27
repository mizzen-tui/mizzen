package mizzen.components;

import mizzen.components.frameset.FrameSet;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import junit.framework.TestCase;

public class MakeFrameSetTest  extends TestCase {

    public void testMakeBasicFrameSetComponent() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        FrameSet fs = makeSimpleFrameSet();

        fs.getSegmentsBuffer().getSegments();

    }
    
    public void testMakeMoreComplicatedFrameSetComponent() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        FrameSet fs = makeMoreComplicatedFrameSet();

        fs.getSegmentsBuffer().getSegments();

    }

    public void testMakeFrameSetAndResizeSmallWidth() {

        makeWFrameSetAndResizeSmallWidth(6);

    }

    public void testMakeFrameSetAndResizeTinyWidth() {

        makeWFrameSetAndResizeSmallWidth(3);

    }

    public void testMakeFrameSetAndResizeSmallHeight() {

        makeWFrameSetAndResizeSmallHeight(6);

    }
    
    public void testMakeFrameSetAndResizeTinyHeight() {

        makeWFrameSetAndResizeSmallHeight(3); // less than 3 crashes ... need to consider introducing a min rectangle

    }
    
    
    public void testMakeWFrameSetAndResizeReallySmallWidth() {
        
        makeWFrameSetAndResizeSmallWidth(4);
        
    }
    
    private void makeWFrameSetAndResizeSmallWidth(int widthToResize) {
        makeWFrameSetAndResize(-1, widthToResize);
    }
    
    private void makeWFrameSetAndResizeSmallHeight(int heightToResize) {
        makeWFrameSetAndResize(heightToResize, -1);
    }
    
    private void makeWFrameSetAndResize(int heightToResize, int widthToResize) {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        FrameSet fs = makeMoreComplicatedFrameSet();

        fs.getSegmentsBuffer().getSegments();
        

        fs.resize(new Dimension.Simple(heightToResize<0?fs.getHeight(): heightToResize, widthToResize<0?fs.getWidth(): widthToResize));
        fs.getSegmentsBuffer().getSegments();
        
    }
    
    public static FrameSet makeSimpleFrameSet() {
        // make the frameset and its child component
        FrameSet fs = new FrameSet(Colors.BIWhi, new Dimension.Simple(10,20));
        fs.setBorder(true);
        fs.setEmptyColor(Colors.On_IBla);
        fs.setFillOnClear(true);

        Component<?> first = new TextComponent("ABC-123");
        fs.add(first);
        return fs;
    }

    public static FrameSet makeMoreComplicatedFrameSet() {

        FrameSet fs = new FrameSet(Colors.BIWhi, new Dimension.Simple(10,20));
        fs.setBorder(true);
        fs.setEmptyColor(Colors.On_IBla);
        fs.setFillOnClear(true);

        Component<?> first = new TextComponent("ABC-123");
        fs.add(first);

        Component<?> second = new TextComponent("XYZ");
        fs.addRight(second, first);

        TextComponent third = new TextComponent("3rd one ...............");
        third.setEmptyColor(Colors.On_Yel);
        third.setFillOnClear(true);
        
        fs.addRight(third, second);

        fs.onResizeExpandRight(first, 33);
        fs.onResizeExpandRight(second, 33);
        fs.onResizeExpandDownAndRight(third);

        return fs;
    }
    
    
}
