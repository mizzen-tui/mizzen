package dtb.terminalIO.examples.plot.image;

import java.awt.image.BufferedImage;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.examples.plot.image.RenderFromGraphics2D.AnimatorController;
import dtb.terminalIO.examples.plot.image.RenderFromGraphics2D.ImageIterator;
import dtb.terminalIO.geom.Dimension;

/**
 * Display some Ascii Fractals..
 * 
 * Demonstrates converting java image objects into ASCII (or Braille) images
 * 
 * @author dbalme
 *
 */
public class FractalsViewer {

    
    public static void main(String[] args) {
        new TUIApp(args)
        .setCanvasSize(new Dimension.Simple(50,120))
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .enableMouseHandling()
        .setOnReadyStart((tApp)->{

            Composite fractalView = new Composite();

            tApp.getMainApplicationPanel().add(0,0, fractalView);

            ButtonBar bannerPanel = new ButtonBar().setHorizontalSpacer(1);
            bannerPanel.setBorder(false);
            bannerPanel.setNoBorderChars();

            bannerPanel.addComponent(new TextComponent(new FGBGColors(Colors.BCya,Colors.On_Whi),"Fractals in ASCII"));

            DropdownListBox<String> gradientsDDLB = new DropdownListBox<String>(ImageViewerFactory.standardAsciiGradients());
            gradientsDDLB.setMaxItemWidth(15);
            gradientsDDLB.setSelected(0);
            bannerPanel.addComponent(gradientsDDLB);

            fractalView.add(bannerPanel);

            Dimension imgSize = new Dimension.ComputedDelegate(tApp.getCanvas(),(s)->s.getHeight()-1, (s)->s.getWidth());
            AnimatorController animator = new AnimatorController(Colors.DEFAULT_COLOR, 
                                                                 new ImageIterator() {
                                                                    @Override
                                                                    public boolean hasNext() {
                                                                        return true;
                                                                    }
                                                                    @Override
                                                                    public BufferedImage next() {
                                                                        return FractalGenerator.makeFractalImage();
                                                                    }}, 
                                                                 imgSize, 
                                                                 tApp.getEventBus(),
                                                                 500);
            fractalView.add(1,0, animator);

            tApp.appendOnResizeAction((tApp2, size)->{
                fractalView.resize(size);
            });


            gradientsDDLB.onChange((te)->{
                animator.getImageRenderRequest().setGradientStr( gradientsDDLB.getValue() );
            });


            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                    tApp.getEventBus(),
                    ()-> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false));

            tApp.getEventBus().appendEventHandler( mainFocusManager );
            
        })
        .start();
    }
    
    

    
}
