package dtb.terminalIO.examples;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.scroll.ScrollableComponent2;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.ColorParsingUtils;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

public class ReadTextFileToScreenTest {
    public static void main(String[] args) throws Exception {
        
        Logging.setEnabled(true);

        String fileData = getFileData(args);

        new TUIApp(args)

        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            AtomicBoolean stripColorCodes = new AtomicBoolean(false);

            Color viewerTextColor = new Colors.FGBGColors(Colors.BIYel, Colors.On_Blu);

            TextComponent image = new TextComponent(fileData) {
                @Override
                public void draw(Color color, IScreenLineSegmentsBuffer segments) {
                     Logging.info("img draw time: "+ 
                                   ElapsedTime.onRunOf( ()-> super.draw(color, segments))+
                                  "  size = "+new Rectangle.Simple(this));
                }
            };
            image.setFillOnClear(true);
            image.setEmptyColor(viewerTextColor);
            image.setColor(viewerTextColor);

            ButtonBar bannerPanel = new ButtonBar().setHorizontalSpacer(0);
            bannerPanel.setBorder(false);
            bannerPanel.setNoBorderChars();

            bannerPanel.addComponent(new TextComponent("Demonstrate reading color codes from text file"));
            bannerPanel.addButton(Colors.IGre,"CTRL-C",Keys.CTRL_C_KEYSTROKE, (te) -> tApp.getTerminateAppHotKeyEventHandler().handle(te)  );
            bannerPanel.addComponent(new TextComponent(" to quit "));

            Button toggleColors = new Button(Colors.IGre,"No colors");

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

            ScrollableComponent2 sc = new ScrollableComponent2(viewerTextColor, 
                                                             (Dimension)tApp.getMainApplicationPanel(), 
                                                             image);
            sc.setFillOnClear(true);
            sc.setEmptyChar('░');

            sc.resize(  new Dimension.Simple(sc.getHeight()-4, sc.getWidth()));

            tApp.getMainApplicationPanel().add(1,1, bannerPanel);

            tApp.getMainApplicationPanel().add(4,0, sc);

            BiConsumer<TUIApp, Dimension> baseResizeAction = tApp.getResizeAction();

            tApp.setOnResizeAction( (ptApp, newSize) ->{
                baseResizeAction.accept(ptApp, newSize);
                sc.resize(  new Dimension.Simple(newSize.getHeight()-4, newSize.getWidth()));
            } );

        }).start();

    }

    private static String getFileData(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Filename not specified");
            System.exit(1);
        }

        String fileName = args[0]; 

        if (fileName.equalsIgnoreCase("GENERATE")) {
            return genTextData();
        }

        // TODO: validation required (file exists etc)

        byte[] fileByteData = Files.readAllBytes(Paths.get(fileName));

        //String fileData  =  new String(fileByteData,StandardCharsets.UTF_8);
        String fileData = new String(fileByteData,StandardCharsets.UTF_8).replace(ControlCode.ERASE_END_OF_LINE.asString(),"");
        fileData = fileData.replace("\t","    ");
        return fileData;
    }

    private static String genTextData() {

        Color colors[] = new Color[] {
                new  Colors.FGBGColors(Colors.BBlu, Colors.On_Gre),
                new  Colors.FGBGColors(Colors.BBlu, Colors.On_IWhi),
                new  Colors.FGBGColors(Colors.BGre, Colors.On_Yel),
                new  Colors.FGBGColors(Colors.BYel, Colors.On_Cya),
                new  Colors.FGBGColors(Colors.BWhi, Colors.On_Pur),
                new  Colors.FGBGColors(Colors.BCya, Colors.On_Bla),
                new  Colors.FGBGColors(Colors.BYel, Colors.On_IBlu),
                new  Colors.FGBGColors(Colors.BBla, Colors.On_Whi)
        };

        StringBuilder sb = new StringBuilder();
        String digits = "0123456789";
        String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String chars = digits + alpha + alpha.toLowerCase();
        for (int i=0 ; i<chars.length(); i++) {
            sb.append(colors[i % colors.length].asString())
              .append(StringUtils.repeat(chars.charAt(i),100))
              .append(Colors.RCol.asString())
              .append('\n');
        }
        sb.append(sb.toString());
        return sb.toString();
    }

}
