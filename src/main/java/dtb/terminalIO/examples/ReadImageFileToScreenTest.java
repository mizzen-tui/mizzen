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
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBPlotPredicate;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBToAnsiColorControlCode;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.events.AbstractTimedRequestedController;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.ElapsedTime;
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

            Dimension desiredImgSize = new Dimension.Simple(tApp.getMainApplicationPanel().getHeight()-8,tApp.getMainApplicationPanel().getWidth()-4);
            
            ButtonBar bannerPanel = new ButtonBar().setHorizontalSpacer(1);
            bannerPanel.setBorder(false);
            bannerPanel.setNoBorderChars();

            bannerPanel.addComponent(new TextComponent(new FGBGColors(Colors.BCya,Colors.On_Whi),"Image to ASCII"));

            Button updateToggler = new Button(Colors.IYel,"             ");
            updateToggler.setHotKey(Keys.CTRL_U_KEYSTROKE);
            updateToggler.setDisplayHotKey(true);
            KeyInputEventHandler toggleUpdateHandler = TestsCommon.toggleScreenupStrategyKeyboardHandling(Keys.CTRL_U_KEYSTROKE, 
                                                                                                          tApp.getCanvas(), 
                                                                                                          tApp.getEventBus(), 
                                                                                                          (u)->{
                                                                                                              updateToggler.setButtonText(String.format("Update: %-20s",String.valueOf(u)));
                                                                                                          });
            updateToggler.onChange(toggleUpdateHandler);
            bannerPanel.addComponent(updateToggler);

            DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode> colorMapDDLB = new DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode>(ImageViewerFactory.colorMap());
            colorMapDDLB.setSelected(0);
            bannerPanel.addComponent(colorMapDDLB);

            DropdownListBox<ImageViewerFactory.RGBPlotPredicate> plotPredicatesDDLB = new DropdownListBox<ImageViewerFactory.RGBPlotPredicate>(ImageViewerFactory.plotPointPredicateOptions());
            plotPredicatesDDLB.setSelected(0);
            bannerPanel.addComponent(plotPredicatesDDLB);

            DropdownListBox<String> gradientsDDLB = new DropdownListBox<String>(ImageViewerFactory.standardAsciiGradients());
            gradientsDDLB.setMaxItemWidth(15);
            gradientsDDLB.setSelected(0);
            bannerPanel.addComponent(gradientsDDLB);

            bannerPanel.addButton(Colors.IGre,"CTRL-C",Keys.CTRL_C_KEYSTROKE, (te) -> tApp.getTerminateAppHotKeyEventHandler().handle(te)  );

            bannerPanel.addComponent(new TextComponent(" to quit "));

            HorizontalPanel hp2ndRow = new HorizontalPanel();
            hp2ndRow.setEmptyChar('X');
//            Button useBrailleToggleButton = new Button(Colors.Whi, "Braille Renderer", true);
            DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE> imageRendererTypeDDLB = new DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE>(ImageViewerFactory.imageRenderTypeOptions());
            imageRendererTypeDDLB.setSelected(0);

            hp2ndRow.add(imageRendererTypeDDLB);

            TextComponent gradientLabel = new TextComponent("Current gradient:");
            hp2ndRow.add(gradientLabel);
            TextInput currentGradientTextBox = new TextInput(10).setColor(Colors.IYel);
            currentGradientTextBox.setText("");

            Button reverseGradientText = new Button(Colors.IRed,"Reverse");
            reverseGradientText.setHotKey(Keys.CTRL_R_KEYSTROKE);
            reverseGradientText.setDisplayHotKey(true);

            hp2ndRow.add(reverseGradientText);
            hp2ndRow.add(currentGradientTextBox);

            TextComponent loadingImage = new TextComponent(Colors.BGre, "  ..   ");

            ScrollableComponent sc = new ScrollableComponent(viewerTextColor, 
                                                             (Dimension)tApp.getMainApplicationPanel(), 
                                                             loadingImage,
                                                             ()-> new DefaultFocusManager(Keys.SHIFT_TAB_KEYSTROKE, Keys.TAB_KEYSTROKE).setNavKeysFirst(false));
            sc.setFillOnClear(false);
            //sc.setEmptyChar('░');

            sc.resize(  new Dimension.Simple(sc.getHeight()-4, sc.getWidth()));

            tApp.getMainApplicationPanel().add(1,1, bannerPanel);
            tApp.getMainApplicationPanel().add(2,1, hp2ndRow);

            tApp.getMainApplicationPanel().add(4,0, sc);

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                                                                                             tApp.getEventBus(),
                                                                                             ()-> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false));

            tApp.getEventBus().appendEventHandler( mainFocusManager );

            Consumer<Dimension>  buildImageViewer = (size) -> {

                Dimension newImageSize = new Dimension.Simple(sc.getHeight()-2, sc.getWidth()-2);

                String loadingText = Colors.coloredText(Colors.BGre,"Loading image ..  Size: ") + 
                                     Colors.coloredText(Colors.Yel, String.valueOf(newImageSize));

                sc.setTitle(loadingText);
                tApp.doRefreshCanvasIfModified();

                new Thread(()->{
                    try {
                        RGBPlotPredicate toPlotOrNot        =  plotPredicatesDDLB.getValue();
                        RGBToAnsiColorControlCode colorMap  = colorMapDDLB.getValue();
                        String gradientStr                  = currentGradientTextBox.getValue();
                        if (gradientStr==null || gradientStr.trim().length() == 0) {
                            gradientStr = gradientsDDLB.getValue();
                        }

                        Component<?> imgViewer;
                        ElapsedTime imgGenerateTime = ElapsedTime.start();
                        if (toPlotOrNot != null && colorMap != null && gradientStr != null) {
                            if (imageRendererTypeDDLB.getValue().equals(IMAGE_RENDER_TYPE.BLOCK)) {
                                imgViewer = ImageViewerFactory.simpleImageToBlocksView(imageToView, newImageSize, toPlotOrNot, colorMap);
                            } else  if (imageRendererTypeDDLB.getValue().equals(IMAGE_RENDER_TYPE.BRAILLE)) {
                                imgViewer = ImageViewerFactory.simpleImageToBrailleView(imageToView, newImageSize, toPlotOrNot, colorMap);
                            } else if (imageRendererTypeDDLB.getValue().equals(IMAGE_RENDER_TYPE.ASCII)) {
                                imgViewer = ImageViewerFactory.simpleImageToStringView(imageToView, newImageSize, toPlotOrNot, colorMap, gradientStr);
                            } else {
                                imgViewer = new TextComponent("Image renderer type not defined");
                            }
                        } else {
                            imgViewer = ImageViewerFactory.fromImage(imageToView, newImageSize);
                        }
                        Logging.info("\n\n-------------------------\ngenerated image time: " + imgGenerateTime.elapsed()+"\n--------------------------------\n");
                        tApp.getEventBus().appendIdleEventHandler( new OnIdleEventEventHandler.OneTime((te)-> {
                            sc.setTitle("");
                            sc.setComponentBeingViewed(imgViewer);
                            te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(sc) );
                        } ));

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            };

            buildImageViewer.accept(desiredImgSize);

            BiConsumer<TUIApp, Dimension> baseResizeAction = tApp.getResizeAction();

            tApp.setOnResizeAction( (ptApp, newSize) ->{
                baseResizeAction.accept(ptApp, newSize);
                sc.resize(  new Dimension.Simple(newSize.getHeight()-4, newSize.getWidth()));
                currentGradientTextBox.resize(new Dimension.Simple(1,sc.getWidth()-20));
                buildImageViewer.accept(newSize);
            } );

            AbstractTimedRequestedController throttledOptionChangedHandler = (new AbstractTimedRequestedController(()->buildImageViewer.accept((Dimension)sc)) {
                @Override
                protected boolean isRequestDifferentFromPrevious() {
                    return true;
                }
                
            }).setTimerDelayMillis(500);

            EventHandler onOptionChanged = (te)->throttledOptionChangedHandler.onRequest(te.getEventBus());

            plotPredicatesDDLB.onChange(onOptionChanged);
            colorMapDDLB.onChange(onOptionChanged);

            gradientsDDLB.onChange((te)->{
                                            currentGradientTextBox.setText( gradientsDDLB.getValue() );
                                            onOptionChanged.handle(te);
                                         });

            currentGradientTextBox.onChange(onOptionChanged);

            reverseGradientText.onChange((te)->{
                StringBuilder sb = new StringBuilder(currentGradientTextBox.getValue());  
                currentGradientTextBox.setText(sb.reverse().toString());
                onOptionChanged.handle(te);
            });
            
            imageRendererTypeDDLB.onChange((te)->{
                if (imageRendererTypeDDLB.getValue().equals(IMAGE_RENDER_TYPE.BLOCK) ||
                    imageRendererTypeDDLB.getValue().equals(IMAGE_RENDER_TYPE.BRAILLE)) {
                    currentGradientTextBox.hide();
                    gradientLabel.hide();
                    reverseGradientText.hide();
                    gradientsDDLB.hide();
                    //tApp.doRefreshCanvasIfModified();
                } else {
                    currentGradientTextBox.show();
                    gradientLabel.show();
                    reverseGradientText.show();
                    gradientsDDLB.show();
                    //tApp.doRefreshCanvasIfModified();
                }
                onOptionChanged.handle(te);
            });

            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

            ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)tApp.getCanvas().getCanvasBackend());

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

        if (fileName.equalsIgnoreCase("GENERATESHAPES")) {
            return genImageData2();
        }

        // TODO: validation required (file exists etc)

        // byte[] fileByteData = Files.readAllBytes(Paths.get(fileName));

        BufferedImage image = ImageIO.read(new File(fileName));
        return image;
    }
    
    private static Image genImageData() {
        return getCircle(500,600);
    }

    private static Image genImageData2() {
        BufferedImage image = new BufferedImage(500, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();
        
      //  BasicShapes.doDrawing(graphics2D);
        
        graphics2D.setColor(java.awt.Color.WHITE);
        graphics2D.setStroke(new BasicStroke(5));
        
        graphics2D.drawLine(10, 20, 500, 200);
        graphics2D.drawLine(500, 200, 10, 100);
        //graphics2D.drawLine(10, 20, 500, 200);
        
        
        return image;
    }
    
    static BufferedImage getCircle(int width, int height) {

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Color transparent = new java.awt.Color(0x00FFFFFF, true);

        java.awt.Color fg = java.awt.Color.black;
        
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        //trying to make the bufferedImage transparent 
        g.setComposite(AlphaComposite.Src);
        g.setColor(transparent);
        g.setBackground(transparent);
        g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        //drawing the circle 
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        float dash1[] = {10.0f};
        BasicStroke dashed =
                new BasicStroke(1.0f,
                                BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER,
                                10.0f, dash1, 0.0f);
        g.setStroke(dashed);        
        
        return bufferedImage;
    }
    
    
    
    
}
