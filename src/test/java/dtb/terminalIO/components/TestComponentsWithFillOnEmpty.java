package dtb.terminalIO.components;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.supportfortesting.CanvasForTesting;
import dtb.terminalIO.textsegments.TestSegmentsFillOnEmpty;
import junit.framework.TestCase;

public class TestComponentsWithFillOnEmpty extends TestCase {

    
    Canvas buildCanvasForTesting() throws Exception {
        return CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput();
    }
    

    public void testSimpleComponentWithFillOnEmpty() throws Exception {

        Canvas canvas = buildCanvasForTesting();
        canvas.setEmptyAttributes(Colors.IRed, 'O');

        Composite mainPanel = new Composite(Colors.IWhi, new Rectangle.Simple(canvas));
        mainPanel.setEmptyChar(TestSegmentsFillOnEmpty.fillChar);
        mainPanel.setFillOnClear(true);

        canvas.addComponentSegments(mainPanel);

        canvas.refresh();

        TestSegmentsFillOnEmpty.assertCharsEverywhere(mainPanel.getSegmentsBuffer().getSegments(), new Rectangle.Simple(mainPanel), true);

    }

    public void testSimpleComponentWithFillOnEmptyAndBorderComponentWithFillTrueAndTextComponent() throws Exception {

        Canvas canvas = buildCanvasForTesting();
        canvas.setEmptyAttributes(Colors.IRed, 'O');

        Composite mainPanel = new Composite(Colors.IWhi, new Rectangle.Simple(canvas));
        mainPanel.setEmptyChar(TestSegmentsFillOnEmpty.fillChar);
        mainPanel.setFillOnClear(true);

        TextComponent tc = new TextComponent("Here is some Text\nLine1\nLine2");

        BorderedComponent bc = new BorderedComponent(tc).setEmptyChar('Z').setFillOnClear(true);;

        mainPanel.add(bc);

        canvas.addComponentSegments(mainPanel);

        canvas.refresh();

        TestSegmentsFillOnEmpty.assertCharsEverywhere(mainPanel.getSegmentsBuffer().getSegments(), new Rectangle.Simple(mainPanel));

    }

    public void testSimpleComponentWithFillOnEmptyAndBorderComponentAndTextComponentWithFillTrue() throws Exception {

        Canvas canvas = buildCanvasForTesting();
        canvas.setEmptyAttributes(Colors.IRed, 'O');

        Composite mainPanel = new Composite(Colors.IWhi, new Rectangle.Simple(canvas));
        mainPanel.setEmptyChar(TestSegmentsFillOnEmpty.fillChar);
        mainPanel.setFillOnClear(true);

        TextComponent tc = new TextComponent("Here is some Text\nLine1\nLine2").setEmptyChar('Y').setFillOnClear(true);
        TestSegmentsFillOnEmpty.printTS(tc.getSegmentsBuffer().getSegments(), new Rectangle.Simple(tc));
        //TestSegmentsFillOnEmpty.assertCharsEverywhere(tc.getSegmentsBuffer().getSegments(), new Rectangle.Simple(tc));

        System.out.println("---------");
        BorderedComponent bc = new BorderedComponent(tc);
        TestSegmentsFillOnEmpty.printTS(bc.getSegmentsBuffer().getSegments(), new Rectangle.Simple(bc));
        TestSegmentsFillOnEmpty.assertCharsEverywhere(bc.getSegmentsBuffer().getSegments(), new Rectangle.Simple(bc));

        mainPanel.add(bc);

        canvas.addComponentSegments(mainPanel);

        canvas.refresh();

        TestSegmentsFillOnEmpty.assertCharsEverywhere(mainPanel.getSegmentsBuffer().getSegments(), new Rectangle.Simple(mainPanel));

    }

}
