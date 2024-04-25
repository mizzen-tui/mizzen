package dtb.terminalIO.examples;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DraggableTextComponent;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

public class DraggableTest {


    public static void main(String args[]) {

        //Composite.DRAW_VIA_PARTS = false;
        
        Logging.setEnabled(true);

        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .setEmptyColors(Colors.On_IYel)
        .confirmOnExit()
        .setOnReadyStart((tApp)->{

            tApp.getMainApplicationPanel().add(10, 3, new BorderedComponent( new DraggableTextComponent(Colors.Red,"RED")));

            ((Composite)tApp.getMainApplicationPanel()).setTitle(new FGBGColors(Colors.BIBlu, Colors.On_IYel), "DraggableTextComponent Examples")
                                                       .setBorderColor(new FGBGColors(Colors.BIBlu, Colors.On_IYel));

            Button quitButton = new Button(new FGBGColors(Colors.BIBlu, Colors.On_Yel),  "Quit")
                                                  .setHotKey(Keys.CTRL_C_KEYSTROKE)
                                                  .setDisplayHotKey(true)
                                                  .setUseCursor(false);
            tApp.getMainApplicationPanel().add(PositionRelativeFactory.fromAlign(VAlign.TOP, Align.RIGHT, ((Composite)tApp.getMainApplicationPanel()).getInnerRectangle(), quitButton), quitButton);
            quitButton.onChange(tApp.getTerminateAppHotKeyEventHandler());
            
            tApp.getMainApplicationPanel().add(10, 13, new BorderedComponent(Colors.IWhi, new DraggableTextComponent(Colors.BIBlu,"BLUE")));

            tApp.getMainApplicationPanel().add(14, 3, new BorderedComponent(new DraggableTextComponent(Colors.BIPur,"     Freeze ROW-DRAG \n <--  Only moves left  --> \n      and right").freezeRow()));

            tApp.getMainApplicationPanel().add(20, 3, new BorderedComponent(new DraggableTextComponent(Colors.BCya,"         ^\n         |\n Freeze COLUMN-DRAG \n  Only moves up\n    and down\n         |\n         v").freezeColumn()));

            String largeText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis nulla elit, placerat sed aliquam at, porta ut justo. Sed vel fringilla nisi, eu laoreet dui. Sed gravida at ipsum et viverra. Ut diam justo, imperdiet sed ante eu, elementum pellentesque sem. Curabitur eleifend turpis eros, nec semper est ultricies eget. Sed sed faucibus mauris, sed porttitor mauris. Ut quis tellus ultrices sapien porttitor tincidunt eu sed risus. Integer ac imperdiet dolor, at ultricies odio. Vestibulum sed pretium ligula. Sed dapibus sapien nunc, eget molestie mauris hendrerit id. Duis blandit tortor eget risus sagittis, at sodales felis congue. Donec vulputate massa aliquam, pretium ante eu, hendrerit ante. Suspendisse potenti. Nam gravida tempus ipsum, non convallis turpis malesuada id. Sed iaculis iaculis condimentum. Fusce pharetra euismod efficitur.";

            tApp.getMainApplicationPanel().add(14, 43, new BorderedComponent(new DraggableTextComponent(Colors.BGre,StringUtils.wrap(largeText, 50))).setTitle("Large Text"));

            LoadedFile fileDetails = LoadedFile.getFileData(args);
            tApp.getMainApplicationPanel().add(35, 43, new BorderedComponent(new DraggableTextComponent(Colors.BGre,fileDetails.fileNameContent)).setTitle(fileDetails.fileName));

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
