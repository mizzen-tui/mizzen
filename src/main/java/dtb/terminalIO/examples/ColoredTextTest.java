package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.UTIL.SegmentsFromStringResult;

public class ColoredTextTest {

    public static void main(String[] args) throws IOException {

        TUIApp app = new TUIApp().initFromArgs(args).setDefaultMainApplicationPanel();


        String fileName = "colour-banner.txt";

        SegmentsFromStringResult r = new SegmentsFromStringResult(null, null);


        Dimension maxDim = new Dimension.Simple(app.getCanvas().getHeight() - 4, app.getCanvas().getWidth() - 4);
        Dimension textSize = Dimension.UTIL.stringToDimension(ReadSampleFiles.readSampleFile(fileName, true));
        Dimension dim = maxDim.contains(textSize) ? textSize : maxDim;

        @SuppressWarnings("rawtypes")
        BoundedComponent image = new BoundedComponent(null, dim) {
            @Override
            protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
                if (r.ts == null) {
                    return;
                }
                segments.merge(r.ts);
            }
        };

        BorderedComponent imageWithBorder = new BorderedComponent(image);
        
        app.setUpdateCanvas((tApp)-> {
            
            if (imageWithBorder.isDirty()) {
                tApp.clearCanvas();
                tApp.getCanvas().addComponentSegments(2, 2, imageWithBorder);
            }
            return true;
        });

        app.setOnReadyStart((tApp) -> {

            app.getCanvas().t("HELLO WORLD!");

            boolean[] stripColors = new boolean[] {true};

            tApp.getEventBus().appendEventHandler(new OnIdleEventEventHandler.Countdown(500, 10, (te, count) -> {

                String text;
                try {
                    text = ReadSampleFiles.readSampleFile(fileName, stripColors[0]);
                    SegmentsFromStringResult rr = ScreenLineSegment.UTIL.textSegmentsFromStringWithEmbeddedColor(text, 2, 0,
                                                                                   new Rectangle.Simple(0, 0, tApp.getCanvas()));
                    r.pos = rr.pos;
                    r.ts = rr.ts;

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                image.clear();
                imageWithBorder.clear();

                stripColors[0] = !stripColors[0];
               
                te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(tApp));
                
                if (count == 1) {
                    te.getEventBus().raiseEvent( new TerminalEvent<Object>(tApp, TYPE.TERMINATE, null) );
                }
                

            }));

        });
        
        app.start();

    }

}