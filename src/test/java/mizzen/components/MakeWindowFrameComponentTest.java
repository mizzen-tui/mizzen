package mizzen.components;

import mizzen.components.composite.Composite;
import mizzen.components.frameset.FrameSet;
import mizzen.components.input.ListBox;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import junit.framework.TestCase;

/**
 * Please note:  These tests create components and cause the component "draw" methods to get indirectly invoked
 *               by calls to component.getSegmentsBuffer().getSegments();
 *               
 *               But where are the asserts?   
 *
 *              The asserts are done indirectly by setting the following
 *              
 *              Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;
 *              
 *              With this flag turned on, the library itself checks that segments do not overlap
 *
 *
 * 
 * @author dbalme
 *
 */
public class MakeWindowFrameComponentTest extends TestCase {

    /** the tests here turn the strict layout check on: off again after each, so other tests don't depend on running after these */
    @Override
    protected void tearDown() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = false;
    }
    
    public void testMakeBasicWindowFrameComponent() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", new TextComponent("ABC-123"));
        wf.getSegmentsBuffer().getSegments();

    }

    public void testMakeBasicWindowFrameWithAFrameSetComponent() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        // make the window frame
        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", MakeFrameSetTest.makeSimpleFrameSet());

        // cause a draw to occur
        wf.getSegmentsBuffer().getSegments();

    }

    public void testMakeBasicWindowFrameWithResize() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        TextComponent tc = new TextComponent("Hello world");

        // make the window frame
        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", tc);

        // cause a draw to occur
        wf.getSegmentsBuffer().getSegments();

        tc.setTextAndResize("Hello world? Why not ..\nHello Dolly?\n\n");

        // cause a draw to occur after resize
        wf.getSegmentsBuffer().getSegments();

    }

    public void testMakeWindowFrameWithListBoxAndResizeChildSmall_25() {
        
        
        makeWindowFrameWithListBoxAndResize(25,25);

    }

    public void testMakeWindowFrameWithListBoxAndResizeChildSmall_20() {

        makeWindowFrameWithListBoxAndResize(20,20);

    }

    private void makeWindowFrameWithListBoxAndResize(int heightToResize, int widthToResize) {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        ListBox<String> lb = ListBox.fromStrings(new String[] { "Silly Sally",
                                                                "Sold sea shells",
                                                                "at the sea shore"});

        // make the window frame
        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", lb);

        // cause a draw to occur
        wf.getSegmentsBuffer().getSegments();

        wf.resize(new Dimension.Simple(heightToResize<0?wf.getHeight(): heightToResize, widthToResize<0?wf.getWidth(): widthToResize));

        // cause a draw to occur after resize
        wf.getSegmentsBuffer().getSegments();

    }

    public void testMakeBasicWindowFrameWithFrameSetAndResizeListBoxChild() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        FrameSet fs = MakeFrameSetTest.makeSimpleFrameSet();
        
        // make the window frame
        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", fs);

        // cause a draw to occur
        wf.getSegmentsBuffer().getSegments();

        fs.resize(new Dimension.Simple(90,90));

        // cause a draw to occur after resize
        wf.getSegmentsBuffer().getSegments();

    }

    
    public void testMakeWindowFrameWithInComposite() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        // make the window frame
        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", MakeFrameSetTest.makeSimpleFrameSet());

        Composite container = new Composite();

        container.add(1,1, wf);

        // cause a draw to occur
        container.getSegmentsBuffer().getSegments();
    }

    public void testMakeWindowFrameWithInComposite2() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        // make the window frame
        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", MakeFrameSetTest.makeMoreComplicatedFrameSet());

        Composite container = new Composite();

        container.add(1,1, wf);

        // cause a draw to occur
        container.getSegmentsBuffer().getSegments();

    }

    
    
    public void testMakeWindowFrameWithInComposite2WithChildResize() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;
        
        
        FrameSet fs = MakeFrameSetTest.makeMoreComplicatedFrameSet();

        // make the window frame
        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", fs);

        Composite container = new Composite();

        container.add(1,1, wf);
        
        
        // cause a draw to occur
        container.getSegmentsBuffer().getSegments();

        
        fs.resize(new Dimension.Simple(90,90));

        // cause a draw to occur
        container.getSegmentsBuffer().getSegments();

    }

    public void testMakeWindowFrameWithInComposite2WithWindowResizeOneHalf() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;
        
        
        FrameSet fs = MakeFrameSetTest.makeMoreComplicatedFrameSet();

        // make the window frame
        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", fs);

        Composite container = new Composite();

        container.add(1,1, wf);
        
        
        // cause a draw to occur
        container.getSegmentsBuffer().getSegments();

        
        wf.resize(new Dimension.Simple(wf.getHeight() / 2, wf.getWidth() / 2));

        // cause a draw to occur
        container.getSegmentsBuffer().getSegments();

    }
    

    public void testMakeWindowFrameWithInComposite2WithWindowResizeDoubleWidth() {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;
        
        
        FrameSet fs = MakeFrameSetTest.makeMoreComplicatedFrameSet();

        // make the window frame
        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", fs);

        Composite container = new Composite();

        container.add(1,1, wf);
        
        
        // cause a draw to occur
        container.getSegmentsBuffer().getSegments();

        wf.resize(new Dimension.Simple(wf.getHeight(), wf.getWidth() * 2));

        // cause a draw to occur
        container.getSegmentsBuffer().getSegments();

    }
    
    public void testMakeWindowFrameWithInComposite2WithWindowResizeSmallWidth() {
        
        testMakeWindowFrameWithInComposite2WithSmallWidth(6);

    }

    public void testMakeWindowFrameWithInComposite2WithWindowResizeTinyWidth() {
        
        testMakeWindowFrameWithInComposite2WithSmallWidth(3);

    }

    
    public void testMakeWindowFrameWithInComposite2WithWindowResizeSmallHeight() {
        
        testMakeWindowFrameWithInComposite2WithSmallHeight(6);

    }

    public void testMakeWindowFrameWithInComposite2WithWindowResizeTinyHeight() {

        testMakeWindowFrameWithInComposite2WithSmallHeight(2);

    }
    
    
    private void testMakeWindowFrameWithInComposite2WithSmallWidth(int widthToResize) {
        testMakeWindowFrameWithInComposite2WithWindowResize(-1, widthToResize);
    }
    
    private void testMakeWindowFrameWithInComposite2WithSmallHeight(int heightToResize) {
        testMakeWindowFrameWithInComposite2WithWindowResize(heightToResize, -1);
    }
    
    
    private void testMakeWindowFrameWithInComposite2WithWindowResize(int heightToResize, int widthToResize) {

        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;
        
        
        FrameSet fs = MakeFrameSetTest.makeMoreComplicatedFrameSet();

        // make the window frame
        WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of a WindowFrame", fs);

        Composite container = new Composite();

        container.add(1,1, wf);

        // cause a draw to occur
        container.getSegmentsBuffer().getSegments();

        wf.resize(new Dimension.Simple(heightToResize<0?wf.getHeight(): heightToResize, widthToResize<0?wf.getWidth(): widthToResize));

        // cause a draw to occur
        container.getSegmentsBuffer().getSegments();

    }
    
    
}
