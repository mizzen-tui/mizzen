package dtb.terminalIO.examples;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.StringCanvasBackEnd;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegmentsView;
import dtb.terminalIO.util.Logging;

public class ClickableTextComponentTest {

    
    static int lastRefreshTime = 0; 
    
    public static void main(String args[]) {


        Logging.setEnabled(true);
        
        new TUIApp(args).setEmptyChar(' ').setDefaultMainApplicationPanel().confirmOnExit().setOnReadyStart((tApp) -> {

            String viewableTextColorsOfTheRainbow = buildViewableText();
            
            TextComponent banner = new TextComponent("TEST ME");
            
            tApp.getMainApplicationPanel().add(0, 0, banner);
            ((Composite)tApp.getMainApplicationPanel()).setFillOnClear(false);

            TextComponent colorsOfTheRainbow = new TextComponent(viewableTextColorsOfTheRainbow) {
                public void draw(Color color, IScreenLineSegmentsBuffer segments) {
                    super.draw(color, segments);
                }
            };

            colorsOfTheRainbow.setFillOnClear(false);

            ScrollableComponent sc = new ScrollableComponent(Colors.Whi, (Dimension)tApp.getMainApplicationPanel(), colorsOfTheRainbow);
            sc.setFillOnClear(false);
            
            sc.resize(  new Dimension.Simple(sc.getHeight()-1, sc.getWidth()));
            
            tApp.getMainApplicationPanel().add(1,0, sc);
            
            
            tApp.getEventBus().appendEventHandler( sc );
            
            tApp.setUpdateCanvas((tApp2)->{
                
                banner.setTextAndResize( "TEST ME " + String.valueOf(ScreenLineSegmentsView.DEBUG_STATS) );
                
                if (!((Component<?>)tApp.getMainApplicationPanel()).isDirty()) {
                    return false; // false means no gui update
                }


                tApp.clearCanvas();
                tApp.getCanvas().addComponentSegments( (Component<?>)tApp.getMainApplicationPanel() );
 
                return true; // true means updated
            });

            tApp.setOnResizeAction((tSameApp, size)->{
                tApp.clearCanvas();
                tApp.setCanvasSize(size);

                ((Composite)tApp.getMainApplicationPanel()).resize(size);
                sc.resize(  new Dimension.Simple(size.getHeight()-1, size.getWidth()));

            });
            

        }).start();

    }

    static String buildViewableText() {
        
        StringCanvasBackEnd stringBackEnd = new StringCanvasBackEnd();
        
        Canvas master_canvas = new Canvas(stringBackEnd);
        master_canvas.setEmptyChar(' ');

        Component<?> colorsOfTheRainBow = ColorsTest.buildColorsPanel();
        
        master_canvas.setSize(colorsOfTheRainBow);
        
        master_canvas.addComponentSegments(colorsOfTheRainBow);
        
        
        master_canvas.refresh();
        
        return stringBackEnd.toString();
    }

    
    static String buildViewableText2() {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos, true, "UTF-8");

            
            TerminalScreen screen = new TerminalScreen();
            screen.setPrintStream(ps);

            Canvas master_canvas = new Canvas(screen);
            master_canvas.setSize(25, 50);
            master_canvas.setEmptyChar(' ');

            if (ps != null) {
                master_canvas.addComponentSegments(0,1,new TextComponent("hello there!"));
                master_canvas.addComponentSegments(1,0,new TextComponent("Good bye there!"));
                master_canvas.refresh();
                byte[] bytes = baos.toByteArray();
                String outputString = new String(bytes);
                return outputString;
            }

            Component<?> colorsOfTheRainBow = ColorsTest.buildColorsPanel();

            master_canvas.addComponentSegments(colorsOfTheRainBow);
            
            master_canvas.refresh();
            
            String outputString = new String(baos.toByteArray());

            return outputString;

        } catch (Exception e) {
            return e.getMessage();
        }

    }

}
