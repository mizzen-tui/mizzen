package dtb.terminalIO.components;

import java.util.function.Consumer;

import dtb.terminalIO.controlcodes.ControlCode.Color;

public class HiliterSetComponentColor implements Hiliter {

    private ColorHiliter colorHiliter;
    private Consumer<Color> colorSetter;
    
    public HiliterSetComponentColor(Component<?> componentToHilite, ColorHiliter colorHiliter, Consumer<Color> colorSetter) {
        super();
        this.colorHiliter = colorHiliter;
        this.colorSetter = colorSetter;
    }

    public HiliterSetComponentColor(Component<?> componentToHilite, ColorHiliter colorHiliter) {
        this(componentToHilite, colorHiliter, (color)->componentToHilite.setColor(color));
    }
    
    public HiliterSetComponentColor(Component<?> componentToHilite) {
        this(componentToHilite, new ColorHiliter(componentToHilite.getColor()));
    }
    
    public HiliterSetComponentColor(Component<?> componentToHilite,  Consumer<Color> colorSetter) {
        this(componentToHilite, new ColorHiliter(componentToHilite.getColor()), colorSetter);
    }
    
    @Override
    public void hilite() {
        colorHiliter.hilite();
        Color color = colorHiliter.getColor();
        colorSetter.accept(color);
    }

    @Override
    public void unHilite() {
        colorHiliter.unHilite();
        Color color = colorHiliter.getColor();
        colorSetter.accept(color);
    }
    
    

}
