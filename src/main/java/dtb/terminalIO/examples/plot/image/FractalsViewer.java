package dtb.terminalIO.examples.plot.image;

import java.awt.image.BufferedImage;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import dtb.terminalIO.components.plot.image.animate.ImageAnimatorComponent;
import dtb.terminalIO.components.plot.image.animate.ImageIterator;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
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
            
            bannerPanel.addComponent(new TextComponent(" Render style : "));
            
            DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE> imageRendererTypeDDLB = new DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE>(ImageViewerFactory.imageRenderTypeOptions());
            imageRendererTypeDDLB.setSelected(0);
            bannerPanel.addComponent(imageRendererTypeDDLB);

            TextComponent asciiGradientLabel = new TextComponent(" Select gradient : ");
            bannerPanel.addComponent(asciiGradientLabel);

            DropdownListBox<String> gradientsDDLB = new DropdownListBox<String>(Colors.BICya, ImageViewerFactory.standardAsciiGradients());
            gradientsDDLB.setMaxItemWidth(15);
            gradientsDDLB.setSelected(0);
            bannerPanel.addComponent(gradientsDDLB);
            

            fractalView.add(bannerPanel);

            Dimension imgSize = new Dimension.ComputedDelegate(tApp.getCanvas(),(s)->s.getHeight()-1, (s)->s.getWidth());

            ImageAnimatorComponent animator = new ImageAnimatorComponent(Colors.DEFAULT_COLOR, 
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
                                                                 1000);
                                                                 
            fractalView.add(1,0, animator);

            tApp.appendOnResizeAction((tApp2, size)->{
                fractalView.resize(size);
            });


            imageRendererTypeDDLB.onChange((te)->{
                animator.getImageRenderRequest().setImageRenderType( imageRendererTypeDDLB.getValue()  );
                if (imageRendererTypeDDLB.getValue().equals(IMAGE_RENDER_TYPE.ASCII)) {
                    asciiGradientLabel.show();
                    gradientsDDLB.show();
                } else {
                    asciiGradientLabel.hide();
                    gradientsDDLB.hide();
                }
            });

            gradientsDDLB.onChange((te)->{
                animator.getImageRenderRequest().setGradientStr( gradientsDDLB.getValue() );
            });


            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                    tApp.getEventBus(),
                    ()-> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false).setName("mainFocusManager"));

            tApp.getEventBus().appendEventHandler( mainFocusManager );
            
        })
        .start();
    }

}
