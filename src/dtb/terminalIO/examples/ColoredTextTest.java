package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.ScreenLineSegment.UTIL.SegmentsFromStringResult;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;

public class ColoredTextTest {

    public static void main(String[] args) throws IOException {

	Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));

	master_canvas.t("HELLO WORLD!");
	
	String fileName = "acme-white-150.txt";
	
	SegmentsFromStringResult r = new SegmentsFromStringResult(null,null);
	
	boolean stripColors = true;

	Dimension maxDim = new Dimension.Simple(master_canvas.getHeight()-4, master_canvas.getWidth()-4);
	Dimension textSize = Dimension.UTIL.stringToDimension( ReadSampleFiles.readSampleFile(fileName, true));
	Dimension dim = maxDim.contains(textSize) ? textSize : maxDim;
	
	
	BoundedComponent image = new BoundedComponent(null, dim) {

		@Override
		protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
		    segments.merge(r.ts);

		}

	    };


	
	BorderedComponent imageWithBorder = new BorderedComponent(image);
	    
	for (int i = 0; i < 10; i++) {

	    String text = ReadSampleFiles.readSampleFile(fileName, stripColors);

	    SegmentsFromStringResult rr = ScreenLineSegment.UTIL.textSegmentsFromStringWithEmbeddedColor(text, 2, 0,
		    new Rectangle.Simple(0, 0, master_canvas));
	    
	    r.pos = rr.pos;
	    r.ts = rr.ts;

	    image.clear();
	    imageWithBorder.clear();
	    master_canvas.addComponent(2,2,imageWithBorder);

	    master_canvas.refresh();
	    
	    TestsCommon.sleep(500);
	    
	    stripColors = ! stripColors;
	}
    }

}
