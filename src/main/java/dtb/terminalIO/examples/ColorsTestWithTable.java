package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.table.DefaultTableModel;
import dtb.terminalIO.components.table.TableColumn;
import dtb.terminalIO.components.table.TableColumnImpl;
import dtb.terminalIO.components.table.TableColumnRenderer;
import dtb.terminalIO.components.table.TableColumnRenderer.TableColumnRendererFromTextComponent;
import dtb.terminalIO.components.table.TableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;

public class ColorsTestWithTable {
    
    public static void main(String args[]) {
        
        Logging.setEnabledFromArgs(args);
        
        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)->{
            
            tApp.getMainApplicationPanel().add(0,10, new TextComponent("   Named Colors"));
            
            Button markDirty = new Button("Mark Table Dirty, then Refresh");
            tApp.getMainApplicationPanel().add(0,30, markDirty);
            
            Composite colorsOfTheRainbow = new HorizontalPanel().setHorizontalSpacer(1);
            colorsOfTheRainbow.setFillOnClear(false);
            colorsOfTheRainbow.setBorder(true);
            
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

            DefaultTableModel tModel = new DefaultTableModel();
            
            TableComponent combinedColorsTable = new TableComponent(Colors.Gre, new Dimension.Simple(40*3+1,16*8+20),tModel);
            combinedColorsTable.setFillOnClear(false);
            combinedColorsTable.setFillOnClear(true);
            combinedColorsTable.setFillOnClear(false);

            TableColumnRenderer renderer = new ColorAndTextRenderer();

            int columnCount = Colors.getBackgroundColorNames().size();
            List<String> columnNames= new ArrayList<String>();
            for (int c=0; c<columnCount; c++) {
                int preferredWidth = Math.max(2, new Double(Math.random() * 20).intValue());
                TableColumnImpl tc = new TableColumnImpl("C"+c,preferredWidth,"C"+c, 0, Align.CENTER, -1, false, true);
                tc.setPreferredHeight(3);
                tc.setColumnRenderer(renderer);
                combinedColorsTable.addColumn(tc);
                columnNames.add("C"+c);
            }

            tModel.setColumnNames(columnNames);

            markDirty.onChange((te)->{
                combinedColorsTable.markAsDirty();
                te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(combinedColorsTable));
            });
            
            int row = 0;
            Color headerColor = new FGBGColors(Colors.BIWhi, Colors.On_IRed);
            for (int c = 0; c <Colors.getBackgroundColorNames().size(); c++) {
                tModel.setValueAt(row, c, new ColorAndText(headerColor, c+""));
            }
            row = 1;
            for (String fgName : Colors.getForegroundColorNames() ) {
                Colors.FgColor fgColor = Colors.getNamedFGColor(fgName);
                int col = 0;
                for (String bgName : Colors.getBackgroundColorNames()) {
                    Colors.BgColor bgColor = Colors.getNamedBGColor(bgName);
                    Colors.FGBGColors combinedColor = new Colors.FGBGColors(fgColor, bgColor);
                    String text = randomText() ; //String.format("%-"+maxName+"s\n%-"+maxName+"s",fgName, bgName);
                    tModel.setValueAt(row, col, new ColorAndText(combinedColor, text));
                    col++;
                }
                row++;
            }
            colorsOfTheRainbow.add(combinedColorsTable);

            ScrollableComponent sc = new ScrollableComponent(Colors.Whi, tApp.getMainApplicationPanel(), colorsOfTheRainbow);
            sc.setFillOnClear(false);

            sc.resize(  new Dimension.Simple(sc.getHeight()-1, sc.getWidth()));

            tApp.getMainApplicationPanel().add(1,0, sc);

            tApp.setOnResizeAction((tSameApp, size)->{
                tApp.clearCanvas();
                tApp.setCanvasSize(size);

                ((Composite)tApp.getMainApplicationPanel()).resize(size);
                sc.resize(  new Dimension.Simple(size.getHeight()-1, size.getWidth()));

                int fixedWidth = backgroundColors.getWidth() + foregroundColors.getWidth() + 3;

                combinedColorsTable.resize( new Dimension.Simple(size.getHeight()-8, size.getWidth() - (4 + fixedWidth)) );

            });

            // traverse main app panel find focusable's and build a focusManager and attach to Eventbus
            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());

            tApp.getEventBus().appendEventHandler(  mainFocusManager );

            // sanity check
            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

        }).start();
    }

    static class ColorAndText {
        String text;
        Color color;
        public ColorAndText(Color color, String text) {
            super();
            this.text = text;
            this.color = color;
        }
    }

    static class ColorAndTextRenderer  extends TableColumnRendererFromTextComponent  {

        protected void renderObjectOntoComponent(Component<?> componentForRendering2, TableColumn tableColumn, Object cellValue, int preferredWidth, int maxCellHeight, Align align) {
            ColorAndText colorAndText = (ColorAndText) cellValue;
            setCellColor(colorAndText.color);
            Logging.info(" setCellText() : " + ElapsedTime.onRunOf(()-> setCellText(colorAndText.text, preferredWidth, maxCellHeight)));
        }
        
        @Override
        protected void renderHeaderOntoComponent(Component<?> componentForRendering2, TableColumn tableColumn, String cellValue, int preferredWidth, int maxCellHeight, Align align) {
            ColorAndText colorAndText = new ColorAndText(Colors.IGre, cellValue);
            renderObjectOntoComponent(componentForRendering2, tableColumn, colorAndText, preferredWidth, maxCellHeight, align);
        }


//        @Override
//        public IScreenLineSegmentsBuffer render(TableColumn tableColumn, Object cellValue, int preferredWidth, int maxCellHeight, Align align) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public IScreenLineSegmentsBuffer render(TableColumn tableColumn, Object cellValue, int preferredWidth, int maxCellHeight) {
//            // TODO Auto-generated method stub
//            return render(tableColumn, cellValue, preferredWidth, maxCellHeight, tableColumn.getAlign());
//        }
    }

    private static String largeText;

    static String randomText() {
        String text = getBigText();
        int s = new Double(Math.floor(Math.random() * (text.length()*.75))).intValue();
        int len = new Double(Math.random()*(text.length() - (s+1))).intValue();
        String result = text.substring(s,s+len);
        return result;
    }

    static String getBigText() {
        if (largeText != null) {
            return largeText;
        }
        try {
            largeText = ReadSampleFiles.readSampleFile("LoremIpsumLarge.txt");
        } catch (IOException e) {
            largeText = e.toString();
        }  
        return largeText;
    }

}