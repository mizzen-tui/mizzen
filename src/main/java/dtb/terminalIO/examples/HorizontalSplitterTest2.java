package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.frameset.HorizontalSplitter;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.StringUtils;

public class HorizontalSplitterTest2 {

    public static void main(String[] args) throws Exception {

        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            HorizontalSplitter hzp = new HorizontalSplitter(Colors.BCya, new Dimension.Simple(30, 80));

            hzp.addComponent(new TextComponent(Colors.IGre,"LEFT\n"+readLargeSampleFile(40)));
            hzp.addComponent(new TextComponent("RIGHT\n"+readLargeSampleFile(30)));

            tApp.getMainApplicationPanel().add(1,1,hzp);

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
