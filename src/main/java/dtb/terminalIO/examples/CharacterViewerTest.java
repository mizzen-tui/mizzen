package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.geom.Dimension;

public class CharacterViewerTest {

    
    public static void main(String[] args) throws IOException {

        
        TUIApp app = new TUIApp();
        app.initFromArgs(args);
        
        
        TextComponent dataToView = new TextComponent(asciiCodesChart());
        
        Dimension scrollPanelDim = new Dimension() {

            @Override
            public int getWidth() {
                int width = Math.max(40, dataToView.getWidth()+2);
                if (width > app.getDimensions().getWidth()) return app.getDimensions().getWidth() - 10;
                return width;
            }

            @Override
            public int getHeight() {
                int height = Math.max(15, dataToView.getHeight()+2 );
                if (height + 5 > app.getDimensions().getHeight() ) {
                    return app.getDimensions().getHeight() - 5;
                }
                return height;
            }
            
        };
        
        
        ScrollableComponent scrollPanel = new ScrollableComponent(Colors.Blu,scrollPanelDim, dataToView);
        
        scrollPanel.setTitle(Colors.IGre, " ASCII VIEWER ");
        
        app.setUpdateCanvas((theApp)->{
            theApp.getCanvas().addComponentSegments(3,5, scrollPanel);
            //theApp.getCanvas().refresh();
            return true;
        });
        
        scrollPanel.asEventHandler(); // TODO:  fixme!!!!!!
        app.getEventBus().appendEventHandler(scrollPanel);
        
        app.start();
        
        
    }
    
    
    private  static String asciiCodesChart() {
        
        StringBuilder sb = new StringBuilder();
        String blueStr = Colors.Blu.asString();
        String whiteStr = Colors.Whi.asString();
        String yellowStr = Colors.IYel.asString();
        String rcol = ControlCode.RCol.asString();
        
        for (int ch = 33 ; ch < 12000 ; ch++) {
            
            String line = String.format("%s%4d    %s%1s %s ", whiteStr, ch, yellowStr,  String.valueOf((char)ch), rcol);
            
            sb.append(line);
            
            if (ch % 5 == 0) {
                sb.append("\n\n");
            } else {
                sb.append(blueStr+"|  "+rcol);
            }
            
            
        }
        
        return sb.toString();
    }

}
