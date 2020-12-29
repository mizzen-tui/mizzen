package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;

public class ColorsTest {
    
    public static void main(String args[]) {
        
        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)->{
            
            tApp.getMainApplicationPanel().add(0,10, new TextComponent("   Named Colors"));
            
            Composite colorsOfTheRainbow = new HorizontalPanel().setHorizontalSpacer(1);
            colorsOfTheRainbow.setFillOnClear(false);
            
            int maxName = -1;
            
            TextComponent backgroundColors = new TextComponent("");
            StringBuilder sb = new StringBuilder();
            
            for (String name : Colors.getBackgroundColorNames()) {
                Color c = Colors.getNamedBGColor(name);
                sb.append(c.asString() + name + Colors.RCol.asString());
                maxName = maxName>name.length()?maxName:name.length();
                sb.append("\n");
            }
            
            backgroundColors.setTextAndResize( "BACKGROUND COLORS\n" + sb.toString() );
            
            colorsOfTheRainbow.add(backgroundColors);
            
            TextComponent foregroundColors = new TextComponent("");
            sb = new StringBuilder();
            for (String name : Colors.getForegroundColorNames()) {
                Color c = Colors.getNamedFGColor(name);
                sb.append(c.asString() + name + Colors.RCol.asString());
                maxName = maxName>name.length()?maxName:name.length();
                sb.append("\n");
            }
            
            foregroundColors.setTextAndResize( "FOREGROUND COLORS\n" + sb.toString() );
            colorsOfTheRainbow.add(foregroundColors);
            
            
            Composite combinedColors = new Composite();
            combinedColors.setFillOnClear(false);
            
            combinedColors.setBulkAdd(true);
            sb = new StringBuilder();
            int colorRow = 0;
            for (String fgName : Colors.getForegroundColorNames() ) {
                Colors.FgColor fgColor = Colors.getNamedFGColor(fgName);
                int colorCol = 0;
                for (String bgName : Colors.getBackgroundColorNames()) {
                    Colors.BgColor bgColor = Colors.getNamedBGColor(bgName);
                    Colors.FGBGColors combinedColor = new Colors.FGBGColors(fgColor, bgColor);
                    String text = String.format("%-"+maxName+"s\n%-"+maxName+"s",fgName, bgName);
                    TextComponent coloredText = new TextComponent(combinedColor, text);
                    combinedColors.add(colorRow, colorCol, coloredText);
                    colorCol += 1 + coloredText.getWidth();
                    coloredText.setFillOnClear(false);
                }
                colorRow +=3 ;
            }
            combinedColors.setBulkAdd(false);
            
            colorsOfTheRainbow.add(combinedColors);
            
            ScrollableComponent sc = new ScrollableComponent(Colors.Whi, tApp.getMainApplicationPanel(), colorsOfTheRainbow);
            sc.setFillOnClear(false);
            
            sc.resize(  new Dimension.Simple(sc.getHeight()-1, sc.getWidth()));
            
            tApp.getMainApplicationPanel().add(1,0, sc);
            
            
            tApp.getEventBus().appendEventHandler( sc );
            
            tApp.setOnResizeAction((tSameApp, size)->{
                tApp.clearCanvas();
                tApp.setCanvasSize(size);
                
                ((Composite)tApp.getMainApplicationPanel()).resize(size);
                sc.resize(  new Dimension.Simple(size.getHeight()-1, size.getWidth()));
                
            });

            
        }).start();
    }
    
    
    public static Component<?> buildColorsPanel() {
        Composite colorsOfTheRainbow = new HorizontalPanel().setHorizontalSpacer(1);
        colorsOfTheRainbow.setFillOnClear(false);
        
        int maxName = -1;
        
        TextComponent backgroundColors = new TextComponent("");
        StringBuilder sb = new StringBuilder();
        
        for (String name : Colors.getBackgroundColorNames()) {
            Color c = Colors.getNamedBGColor(name);
            sb.append(c.asString() + name + Colors.RCol.asString());
            maxName = maxName>name.length()?maxName:name.length();
            sb.append("\n");
        }
        
        backgroundColors.setTextAndResize( "BACKGROUND COLORS\n" + sb.toString() );
        
        colorsOfTheRainbow.add(backgroundColors);
        
        TextComponent foregroundColors = new TextComponent("");
        sb = new StringBuilder();
        for (String name : Colors.getForegroundColorNames()) {
            Color c = Colors.getNamedFGColor(name);
            sb.append(c.asString() + name + Colors.RCol.asString());
            maxName = maxName>name.length()?maxName:name.length();
            sb.append("\n");
        }
        
        foregroundColors.setTextAndResize( "FOREGROUND COLORS\n" + sb.toString() );
        colorsOfTheRainbow.add(foregroundColors);
        
        
        Composite combinedColors = new Composite();
        combinedColors.setFillOnClear(false);
        
        combinedColors.setBulkAdd(true);
        sb = new StringBuilder();
        int colorRow = 0;
        for (String fgName : Colors.getForegroundColorNames() ) {
            Colors.FgColor fgColor = Colors.getNamedFGColor(fgName);
            int colorCol = 0;
            for (String bgName : Colors.getBackgroundColorNames()) {
                Colors.BgColor bgColor = Colors.getNamedBGColor(bgName);
                Colors.FGBGColors combinedColor = new Colors.FGBGColors(fgColor, bgColor);
                String text = String.format("%-"+maxName+"s\n%-"+maxName+"s",fgName, bgName);
                TextComponent coloredText = new TextComponent(combinedColor, text);
                combinedColors.add(colorRow, colorCol, coloredText);
                colorCol += 1 + coloredText.getWidth();
                coloredText.setFillOnClear(false);
            }
            colorRow +=3 ;
        }
        combinedColors.setBulkAdd(false);
        
        colorsOfTheRainbow.add(combinedColors);
        
        return colorsOfTheRainbow;
    }
    

}
