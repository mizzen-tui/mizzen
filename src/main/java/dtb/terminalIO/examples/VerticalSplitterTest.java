package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.frameset.VerticalSplitter;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

public class VerticalSplitterTest {


    public static void main(String[] args) throws Exception {
        
        Logging.setEnabled(true);

        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            VerticalSplitter vzp = new VerticalSplitter(Colors.BCya, new Dimension.Simple(30, 80));

            vzp.addComponent(new TextComponent(Colors.IGre,"TOP\n This is the top part\nSo there!"));
            vzp.addComponent(new TextComponent("BOTTOM\n"+readLargeSampleFile(30)));
            
            vzp.setTitle(Colors.IBla, "Vertical Splitter Test");

            tApp.getMainApplicationPanel().add(1,1,vzp);

        }).start();
    }

    static String readLargeSampleFile(int wrapLength) {
        String bigFile = "LoremIpsumLarge.txt";
        String largeText = null;
        try {
            largeText = ReadSampleFiles.readSampleFile(bigFile);  
            largeText = StringUtils.wrap(largeText, wrapLength);

            largeText = StringUtils.addLineNumbers(largeText);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return largeText;
    }
    
    
    
}
