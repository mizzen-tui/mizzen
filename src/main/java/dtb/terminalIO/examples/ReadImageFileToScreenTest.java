package dtb.terminalIO.examples;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.imageio.ImageIO;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class ReadImageFileToScreenTest {

    public static void main(String[] args) throws Exception {
        
        Logging.setEnabled(true);

        Image imageToView = getFileData(args);
        
        new TUIApp(args)

        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            AtomicBoolean stripColorCodes = new AtomicBoolean(false);

           //Color viewerTextColor = new Colors.FGBGColors(Colors.BIYel, Colors.On_Blu);
           Color viewerTextColor = Colors.DEFAULT_COLOR;
            
            Dimension desiredImgSize = new Dimension.Simple(tApp.getMainApplicationPanel().getHeight()-6,tApp.getMainApplicationPanel().getWidth()-4);
            
            Function<Dimension, Component<?>>  buildImageViewer = (size) -> {
                
                Component<?> imgViewer = null;
                try {
                    imgViewer = ImageViewerFactory.fromImage(imageToView, desiredImgSize);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(e);
                }
                return imgViewer;
            };
            
            

            Component<?> imageViewer = buildImageViewer.apply(desiredImgSize);
            
            ButtonBar bannerPanel = new ButtonBar().setHorizontalSpacer(0);
            bannerPanel.setBorder(false);
            bannerPanel.setNoBorderChars();

            bannerPanel.addComponent(new TextComponent("Demonstrate reading color codes from text file"));
            bannerPanel.addButton(Colors.IGre,"CTRL-C",Keys.CTRL_C_KEYSTROKE, (te) -> tApp.getTerminateAppHotKeyEventHandler().handle(te)  );
            bannerPanel.addComponent(new TextComponent(" to quit "));

            Button toggleColors = new Button(Colors.IGre,"No colors");
/*
            bannerPanel.addButton(toggleColors,Keys.CTRL_D_KEYSTROKE, (te) -> {
                stripColorCodes.getAndSet(!stripColorCodes.get());
                String data = fileData;
                if (stripColorCodes.get()) {
                    data = ColorParsingUtils.removeColorCodes(data);
                    toggleColors.setButtonText("Show colors");
                } else {
                    toggleColors.setButtonText("No colors");
                }
                image.setText(data);
                tApp.doRefreshCanvasIfModified();
            }  );
*/
            
            ScrollableComponent sc = new ScrollableComponent(viewerTextColor, 
                                                             (Dimension)tApp.getMainApplicationPanel(), 
                                                             imageViewer,
                                                             ()-> new DefaultFocusManager(Keys.SHIFT_TAB_KEYSTROKE, Keys.TAB_KEYSTROKE).setNavKeysFirst(false));
            sc.setFillOnClear(true);
            sc.setEmptyChar('░');

            sc.resize(  new Dimension.Simple(sc.getHeight()-4, sc.getWidth()));
            

            tApp.getMainApplicationPanel().add(1,1, bannerPanel);

            tApp.getMainApplicationPanel().add(4,0, sc);

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                    tApp.getEventBus(),
                    ()-> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false));

            tApp.getEventBus().appendEventHandler( mainFocusManager );

            BiConsumer<TUIApp, Dimension> baseResizeAction = tApp.getResizeAction();

            tApp.setOnResizeAction( (ptApp, newSize) ->{
                baseResizeAction.accept(ptApp, newSize);
                Component<?> imageViewerResized = buildImageViewer.apply(newSize);
                
                sc.setComponentBeingViewed(imageViewerResized);
                
                sc.resize(  new Dimension.Simple(newSize.getHeight()-4, newSize.getWidth()));
            } );

            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

            ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)tApp.getCanvas().getCanvasBackend());
            //canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_FULL);
            
            
        }).start();

    }

    private static Image getFileData(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Filename not specified");
            System.exit(1);
        }
        
        String fileName = args[0]; 

        if (fileName.equalsIgnoreCase("GENERATE")) {
            return genImageData();
        }

        // TODO: validation required (file exists etc)

        // byte[] fileByteData = Files.readAllBytes(Paths.get(fileName));

        BufferedImage image = ImageIO.read(new File(fileName));
        return image;
    }
    
    private static Image genImageData() {
        return getCircle(100,100);
    }
    
    static BufferedImage getCircle(int width, int height) {

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Color transparent = new java.awt.Color(0x00FFFFFF, true);

        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        //trying to make the bufferedImage transparent 
        g.setComposite(AlphaComposite.Src);
        g.setColor(transparent);
        g.setBackground(transparent);
        g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        //drawing the circle 
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(java.awt.Color.red);

        g.setStroke(new BasicStroke(5));
        g.drawOval(0, 0, 100, 100);
        return bufferedImage;
    }
    
    
}
