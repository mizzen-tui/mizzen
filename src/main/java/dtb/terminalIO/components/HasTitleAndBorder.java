package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode.Color;

public interface HasTitleAndBorder<T extends Component<?>> extends Hiliter {
    
    public boolean hasBorder();
    
    public T setBorder(boolean hasBorder);
    
    public T setBorderColor(Color borderColors);

    public T setTitle(Color titleColors, String title);

    public T setTitle(String title);
    
    public String getTitle();

}
