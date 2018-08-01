package dtb.terminalIO.examples;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalLine;
import dtb.terminalIO.components.TextComponent;

public class CompositeTest {
    
    static         int rowLIMIT=28;

    
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));


        build_bounded_pre(master_canvas);
        
        build_bounded_post(master_canvas);
        

        Composite text2 = build_text2(master_canvas);

        
        Composite text3 = build_text3(master_canvas);

        master_canvas.t(0, 3, String.valueOf(text2));
        master_canvas.t(1, 3, String.valueOf(text3));
        
                        
        master_canvas.refresh();

    }

    public static Composite build_bounded_pre(Canvas master_canvas) {
        Composite textC = new Composite()
                                .setBounds(5, 15 )
                                .setTitle(new FGBGColors(Colors.Red, Colors.On_IYel), " Bounded ")
                                .add(new TextComponent(null, "Silly Sally sold sea shells at the\n" + 
                                                             "seashore\n" + 
                                                             "but my - what nice hooters she\n"+
                                                             "had..  very yummy\n"
                                                             + "What can be done?"));
        master_canvas.addComponent(4, 112, textC);
        return textC;
    }
        
     public static Composite build_bounded_post(Canvas master_canvas) {
            Composite textC = new Composite()
                                    //.setBounds(6, 20 )
                                    .setTitle(new FGBGColors(Colors.Red, Colors.On_IYel), " Pre-Bounded ")
                                    .add(new TextComponent(null, "Silly Sally sold sea shells at the\n" + 
                                                                 "seashore\n" + 
                                                                 "but my - what nice hooters she\n"+
                                                                 "had..  very yummy\n"
                                                                 + "What can be done?"))
                                    .t(5,0,"The last one")
                                    .setBounds(6, 20 );
        
        
        master_canvas.addComponent(10, 112, textC);
        return textC;
    }

    public static Composite build_text3(Canvas master_canvas) {
        Composite text3 = new Composite()
                // .setBounds(25, 50 )
                .setTitle(new FGBGColors(Colors.Red, Colors.On_IYel), "Unbounded Frame title:before ")
                .add(new TextComponent(null, "In Spain .. In Spain\n" + 
                                             "The Rain in Spain stays \n" + 
                                             "mainly on the plain!")).t(6,4, "Texxxxxt"); //.t("1-one\n2-two\n3-three\n!");
        

        text3.t(2,2, "@");
        for (int r=0;r<rowLIMIT;r++) {
            
               text3.t(r,r, (r )+"-");
        }
        
        master_canvas.addComponent(2, 75, new HorizontalLine(new FGBGColors(Colors.BGre),20));
        master_canvas.addComponent(3, 75, text3);
        
        master_canvas.setEmptyChar('.');
        return text3;
    }

    public static Composite build_text2(Canvas master_canvas) {
        Composite text2 = new Composite()
                // .setBounds(25, 50 )
                // .setTitle(new FGBGColors(Colors.Red, Colors.On_IYel), " Poem ")
                .add(new TextComponent(null, "In Spain .. In Spain\n" + 
                                             "The Rain in Spain stays \n" + 
                                             "mainly on the plain!")).t(6,4, "Texxxxxt"); //.t("1-one\n2-two\n3-three\n!");
        
        
        text2.t(2,2, "@");
        for (int r=0;r<rowLIMIT;r++) {
            
               text2.t(r,r, (r )+"-");
        }
        
        text2.setTitle(new FGBGColors(Colors.Red, Colors.On_IYel), "Unbounded Frame title:after");
        
       // master_canvas.addComponent(2, 30, textC);
        
        master_canvas.addComponent(2, 35, new HorizontalLine(new FGBGColors(Colors.BGre),20));
        master_canvas.addComponent(3, 35, text2);
        return text2;
    }
}
