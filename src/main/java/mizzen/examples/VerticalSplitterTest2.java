package mizzen.examples;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.frameset.HorizontalSplitter;
import mizzen.components.frameset.VerticalSplitter;
import mizzen.controlcodes.color.Colors;
import mizzen.examples.sampledata.ReadSampleFiles;
import mizzen.geom.Dimension;
import mizzen.util.Logging;
import mizzen.util.StringUtils;

public class VerticalSplitterTest2 {


    public static void main(String[] args) throws Exception {

        Logging.setEnabled(true);

        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            HorizontalSplitter hzp = new HorizontalSplitter(Colors.BCya, new Dimension.Simple(30, 100));

            VerticalSplitter vzp = new VerticalSplitter(Colors.BCya, new Dimension.Simple(30, 80));

            vzp.addComponent(new TextComponent(Colors.IGre,"TOP\n This is the top part\nSo there!"));
            vzp.addComponent(new TextComponent("BOTTOM\n"+readLargeSampleFile(30)));

            hzp.setTitle(Colors.IBla, "Vertical Splitter Test");

            hzp.addComponent(vzp);
            hzp.addComponent(new TextComponent(Colors.IGre,"RIGHT\n"+readLargeSampleFile(30)));

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
