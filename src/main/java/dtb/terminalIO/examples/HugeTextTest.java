package dtb.terminalIO.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegmentsView;

public class HugeTextTest {
    
    
    String readLargeFile(String fileName) throws IOException {
        try (InputStream stream = new FileInputStream(fileName);) {
            return ReadSampleFiles.readStreamToString(stream);
        }
    }

    public static void main(String[] args) throws Exception {
        
        
        HugeTextTest tests = new HugeTextTest();
        
        
        String bigFile = "/home/david/workspace/dtbTerminalIO/lotsOfCableModems.txt";
        
        
        tests.readLargeFile(bigFile);

        
        String hugeStringResult = tests.elapsed( "READ HUGE FILE: ", () -> {
            try {
                return tests.readLargeFile(bigFile);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return bigFile;
        });
        
        String[] result2 = tests.elapsed( "Split" , () -> hugeStringResult.split("\n",-1));
        

        Dimension dimension = tests.elapsed( "Dimension" , () ->  Dimension.UTIL.stringToDimension(hugeStringResult));
    
        boolean colors = tests.elapsed( "Has Colors" , () ->  ControlCode.hasColorCodes(hugeStringResult));

        TextComponent tcomponent = tests.elapsed("TextComponent", () -> new TextComponent(hugeStringResult));
    
        System.out.println( "FillOnClearPolicy :" + tcomponent.getFillOnClearPolicy());
        
    
        IScreenLineSegmentsBuffer segmentsBuffer = tests.elapsed("getSegmentsBuffer()", () -> tcomponent.getSegmentsBuffer() );

        Boolean clearResult = tests.elapsed("clear()", () -> {tcomponent.clear() ; return true;} );

        clearResult = tests.elapsed("setText()", () -> {tcomponent.setText(hugeStringResult) ; return true;} );
        
        System.out.println("-------------------------------------------------------------");

        TextComponent tcomponent2 = tests.elapsed("TextComponent2", () -> new TextComponent(""));
        
        tcomponent2.setFillOnClear(false);

        clearResult = tests.elapsed("tcomponent2.setTextAndResize(HUGEText)", () -> {tcomponent2.setTextAndResize(hugeStringResult) ; return true;} );
        
        
        IScreenLineSegmentsBuffer segmentsBuffer2 = tests.elapsed("tcomponent2.getSegmentsBuffer()", () -> tcomponent2.getSegmentsBuffer() );

        tests.elapsed("tcomponent2.getSegmentsBuffer() - TAKE2 (should be fast!)", () -> tcomponent2.getSegmentsBuffer().getSegments() );
        
        Boolean clearResult2 = tests.elapsed("tcomponent2.clear()", () -> {tcomponent2.clear() ; return true;} );

        tests.elapsed("tcomponent2.getSegmentsBuffer().getSegments() - TAKE3 (after clear)", () -> tcomponent2.getSegmentsBuffer().getSegments() );

        
        // ScreenLineSegmentsView  ScreenLineSegmentsView(IScreenLineSegmentsBuffer.Provider sourceSegments, Position offsetIntoSource, Dimension maxsize)
        
        ScreenLineSegmentsView segmentsView = tests.elapsed("ScreenLineSegmentsView", () -> new ScreenLineSegmentsView(tcomponent2, Position.ZERO, new Dimension.Simple(10,01))  );
        
        
        tests.elapsed("loop segments and count", () -> {

                                                    int counter = 0;
                                                    for (ScreenLineSegment sourceSegment :  tcomponent2.getSegmentsBuffer().getSegments() )  {
                                                        counter++;
                                                    }
                                                     return counter;
                                                    } );
        
        
        tests.elapsed("loop segments hasOverLap", () -> {

            int counter = 0;
            Rectangle bounds = new Rectangle.Simple(Position.ZERO, new Dimension.Simple(50,120));
            for (ScreenLineSegment sourceSegment :  tcomponent2.getSegmentsBuffer().getSegments() )  {
                if (Rectangle.UTIL.hasOverlap(bounds, sourceSegment)) {
                   counter++;
                }
            }
             return counter;
            } );
        
        
        System.out.println("Need to optimize tcomponent2.getSegmentsBuffer().getSegments()!!!  5.2 seconds is way too long!!!");
        
        
    }


    <T> T elapsed(String actionName,Supplier<T> runnable) throws Exception {
        long start = System.currentTimeMillis();
        T result =  runnable.get();
        long now =  System.currentTimeMillis();
        System.out.println(actionName+ " elapsed " + (now - start));
        return result;
    }
    

}
