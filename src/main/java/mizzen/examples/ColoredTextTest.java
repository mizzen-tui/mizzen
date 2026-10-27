package mizzen.examples;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.BorderedComponent;
import mizzen.components.BoundedComponent;
import mizzen.controlcodes.color.Color;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.TerminalEvent.TYPE;
import mizzen.examples.sampledata.ReadSampleFiles;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegment.UTIL.SegmentsFromStringResult;

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
               
                
                if (count == 1) {
                    te.getEventBus().raiseEvent( new TerminalEvent<Object>(tApp, TYPE.TERMINATE, null) );
                }
                

            }));

        });
        
        app.start();

    }

}