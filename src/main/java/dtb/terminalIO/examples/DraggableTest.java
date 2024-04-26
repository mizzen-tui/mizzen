package dtb.terminalIO.examples;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DraggableTextComponent;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.BgColor;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.controlcodes.color.Colors.FgColor;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

public class DraggableTest {


    public static void main(String args[]) {

        Logging.setEnabled(false);

        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .setEmptyColors(Colors.On_IBla)
        .confirmOnExit()
        .setOnReadyStart((tApp)->{
            
            FgColor mainPanel_FG_color = Colors.BWhi;
            BgColor mainPanel_BG_color = Colors.On_IBla;
            Color borderTitleColor = new FGBGColors(mainPanel_FG_color, mainPanel_BG_color);

            tApp.getMainApplicationPanel().add(10, 3, new BorderedComponent( new DraggableTextComponent(Colors.Red,"RED")));

            ((Composite)tApp.getMainApplicationPanel()).setTitle(borderTitleColor, " DraggableTextComponent Examples ")
                                                       .setBorderColor(borderTitleColor);

            Button quitButton = new Button(borderTitleColor,  "Quit")
                                          .setHotKey(Keys.CTRL_C_KEYSTROKE)
                                          .setDisplayHotKey(true)
                                          .setUseCursor(false);

            tApp.getMainApplicationPanel().add(PositionRelativeFactory.fromAlign(VAlign.TOP, Align.RIGHT, ((Composite)tApp.getMainApplicationPanel()).getInnerRectangle(), quitButton), quitButton);
            quitButton.onChange(tApp.getTerminateAppHotKeyEventHandler());

            Color helpTextColor = new FGBGColors(Colors.Whi, mainPanel_BG_color);
            Color helpTextColorBOLD = new FGBGColors(Colors.IWhi, mainPanel_BG_color);
            Component<?> helpText = new BorderedComponent(new TextComponent(helpTextColor,
                                  helpTextColorBOLD.asString()+"        Drag and Drop examples    "+helpTextColor.asString()+"\n"+
                                      helpTextColor.asString()+"        "+ StringUtils.repeat(BlockChars.DEFAULT.getHORIZONTAL_LINE(),22) +" \n" +
                                                               " Drag the various components \n"+
                                                               " around the screen    \n"+
                                                               "\n"+
                                                               " Notice then when you initiate the drag \n"+
                                                               " the component is moved to the top.\n"+
                                                               "\n"+
                                                               " Notice when you drag the FREEZE-ROW\n"+
                                                               " component, it only moves left and right \n"+
                                                               "\n"+
                                                               " Notice when you drag the FREEZE-COL\n"+
                                                               " component, it only moves up and down\n"
                                                       ));
            
            tApp.getMainApplicationPanel().add(PositionRelativeFactory.fromAlign(VAlign.MIDDLE, 0, Align.RIGHT, 5,((Composite)tApp.getMainApplicationPanel()).getInnerRectangle(), helpText), helpText);
            
            
            tApp.getMainApplicationPanel().add(10, 13, new BorderedComponent(Colors.IWhi, new DraggableTextComponent(Colors.BIBlu,"BLUE")));

            tApp.getMainApplicationPanel().add(14, 3, new BorderedComponent(new DraggableTextComponent(Colors.BIPur,"     Freeze ROW-DRAG \n <--  Only moves left  --> \n      and right").freezeRow()));

            tApp.getMainApplicationPanel().add(20, 3, new BorderedComponent(new DraggableTextComponent(Colors.BCya,"         ^\n         |\n Freeze COLUMN-DRAG \n  Only moves up\n    and down\n         |\n         v").freezeColumn()));

            String largeText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis nulla elit, placerat sed aliquam at, porta ut justo. Sed vel fringilla nisi, eu laoreet dui. Sed gravida at ipsum et viverra. Ut diam justo, imperdiet sed ante eu, elementum pellentesque sem. Curabitur eleifend turpis eros, nec semper est ultricies eget. Sed sed faucibus mauris, sed porttitor mauris. Ut quis tellus ultrices sapien porttitor tincidunt eu sed risus. Integer ac imperdiet dolor, at ultricies odio. Vestibulum sed pretium ligula. Sed dapibus sapien nunc, eget molestie mauris hendrerit id. Duis blandit tortor eget risus sagittis, at sodales felis congue. Donec vulputate massa aliquam, pretium ante eu, hendrerit ante. Suspendisse potenti. Nam gravida tempus ipsum, non convallis turpis malesuada id. Sed iaculis iaculis condimentum. Fusce pharetra euismod efficitur.";

            Component<?> largeLoremIpSumComponent = new BorderedComponent(new DraggableTextComponent(new FGBGColors(Colors.IGre, mainPanel_BG_color),StringUtils.wrap(largeText, 50))).setTitle("Large Text");
            tApp.getMainApplicationPanel().add(12, 43, largeLoremIpSumComponent);
            tApp.getMainApplicationPanel().setDropShadow(true, largeLoremIpSumComponent);

            LoadedFile fileDetails = LoadedFile.getFileData(args);
            tApp.getMainApplicationPanel().add(35, 43, new BorderedComponent(new DraggableTextComponent(Colors.IGre,fileDetails.fileNameContent)).setTitle(fileDetails.fileName));

        })
        .start();
    }
    
    static class LoadedFile {
        String fileName;
        String fileNameContent;
        public LoadedFile(String fileName, String fileNameContent) {
            super();
            this.fileName = fileName;
            this.fileNameContent = fileNameContent;
        }

        static LoadedFile getFileData(String[] args)  {
            if (args.length == 0) {
                return new LoadedFile("File-not-specified","Filename not specified\nYou can specify a fileName on command line");
            }
            
            String fileName = args[0]; 
            
            // TODO: validation required (file exists etc)
            
            try {
                byte[] fileByteData = Files.readAllBytes(Paths.get(fileName));
                
                //String fileData  =  new String(fileByteData,StandardCharsets.UTF_8);
                String fileData = new String(fileByteData,StandardCharsets.UTF_8).replace(ControlCode.ERASE_END_OF_LINE.asString(),"");
                fileData = fileData.replace("\t","    ");
                return new LoadedFile(fileName, fileData);
            } catch (IOException e) {
                return new LoadedFile("Error loading file", "File "+fileName +"\n\n"+ e.getMessage());
            }
        }
    }


}
