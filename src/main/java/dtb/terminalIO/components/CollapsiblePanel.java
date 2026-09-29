package dtb.terminalIO.components;

import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.StringUtils;

public class CollapsiblePanel extends AbstractComponentWrappper<VerticalPanel>  {

    static final String show_BUTTON_TEXT = "(+)";
    static final String hide_BUTTON_TEXT = "(-)";

    ButtonBar titleBanner;
    Component<?> panelToHide;
    
    public CollapsiblePanel(Color color, Color borderColor, Component<?> titleComponent, Component<?> panelToHide) {
        
        super(color,new VerticalPanel());
        this.getWrappedComponent().setBorderColor(borderColor);

        this.panelToHide = panelToHide;
        
        titleBanner = new ButtonBar().setNoBorderChars();
        Button showHideNavControlsButton = new Button(Colors.BBla,show_BUTTON_TEXT);
        showHideNavControlsButton.setValue(false);
        

        titleBanner.addComponent(titleComponent);
        titleBanner.addComponent(showHideNavControlsButton);
        
        int panelWidth = panelToHide.getWidth();
        int titleBannerWidth = titleBanner.getWidth();
        
        if (panelWidth > (titleBannerWidth-2)) {
            int spacerWidth = panelWidth - (titleBannerWidth );
            TextComponent spacer = new TextComponent(StringUtils.repeat(" ", spacerWidth));
            titleBanner.insertComponent(1, spacer);
        }

        this.getWrappedComponent().add(titleBanner);
        this.getWrappedComponent().add(panelToHide);
        
        showHideNavControlsButton.onChange((te)->{
            boolean visible = this.getWrappedComponent().isChildVisible(panelToHide);
            showHideNavControlsButton.setValue(visible);
            if (visible) {
                panelToHide.hide();
                showHideNavControlsButton.setButtonText("(+)");
            } else {
                showHideNavControlsButton.setButtonText("(-)");
                panelToHide.show();
            }
        });
        
        panelToHide.hide();

        getWrappedComponent().onResizeExpandRight(titleBanner);
        getWrappedComponent().onResizeExpandDownAndRight(panelToHide);

    }
    
    public CollapsiblePanel(Color color, Component<?> titleComponent, Component<?> panelToHide) {
        this(color, Colors.BBla, titleComponent, panelToHide);
    }

    public CollapsiblePanel(Color color, String titleLabel, Component<?> panelToHide) {
        this(color, Colors.BBla, new TextComponent(titleLabel), panelToHide);
    }

    public CollapsiblePanel(Color color, Color backGroundColor, String titleLabel, Component<?> panelToHide) {
        this(color, backGroundColor, new TextComponent(titleLabel), panelToHide);
    }


    boolean resizing = false;
    @Override
    public void resize(Dimension d) {
        if (resizing) { // super(ancestor) can indirectly call child again - we stop that
            return;
        }
        try {
            if (this.isSameDimension(d)) {
                super.resize(d);
                return ;
            }

            
            titleBanner.resize(new Dimension.Simple(titleBanner.getHeight(),d.getWidth()-2));
            
            if (this.panelToHide instanceof CanResize) {
                ((CanResize) this.panelToHide).resize(new Dimension.Simple(d.getHeight() - ( titleBanner.getHeight()+2),d.getWidth()-2));
            }

            super.resize(d);                    

        } finally {
            resizing = false;
        }
    }
        

}
