package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.CanvasComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;

public class CanvasComponentTest {

    public static void main(String[] args) throws IOException {

        // Logging.setEnabled(true);
        
        new TUIApp(args)
                .setCanvasSize(24, 100)
                .setEmptyChar(' ')
                .setMainApplicationPanel(new Composite().setBounds(20, 100).setEmptyChar(' ').setBorder(true))
                .confirmOnExit()
                .setOnReadyStart((tApp)-> {
                    
                    CanvasComponent canvasComponent = new CanvasComponent();
                    canvasComponent.setSize(tApp.getMainApplicationPanel().getHeight()-2, 
                                            tApp.getMainApplicationPanel().getWidth()-2);
                    
                    canvasComponent.addTextHandleColors("HELLO \n\nCANVAS WORLD\n");
                    canvasComponent.addTextHandleColors(" and here is more text");
                    canvasComponent.addTextHandleColors("\nEven more text");
                    
                    for (int i=10 ; i>=0; i--) {
                        canvasComponent.addTextHandleColors("i = " + i + "\n\n");
                    }
                    
                    
                    tApp.getMainApplicationPanel().add(canvasComponent);
                    

                }).start();
    }
    
}
