
package mizzen.examples;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.BorderedComponent;
import mizzen.components.CollapsibleSidePanel;
import mizzen.components.HasTitleAndBorder;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.theme.Theme;
import mizzen.components.theme.ThemePickerComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;

/**
 * Exercises CollapsibleSidePanel:
 *
 * 1. Expand/collapse using the sidebar's own toggle.
 * 2. Expand/collapse using external buttons.
 * 3. Squeeze the sidebar by resizing it.
 * 4. Observe how HorizontalPanel handles the changes.
 *
 * Enable the component inspector to examine geometry.
 */
public class CollapsibleSidePanelTest {

    public static void main(String[] args) throws IOException {

        new TUIApp(args)
                .setDefaultMainApplicationPanel()
                .setFullScreen(false)
                //.setCanvasSize(20, 80)
                .enableComponentInspector()
                .setOnReadyStart((tApp) -> {

                        Runnable postSetupRunnable = ()->{};
                        if (tApp.getApplicationFocusManager() instanceof HasTitleAndBorder) {
                                ((HasTitleAndBorder<?>)tApp.getApplicationFocusManager()).setBorder(true);
                        }
                        if (tApp.getApplicationFocusManager() instanceof Composite) {
                                Composite appPanel = ((Composite )tApp.getApplicationFocusManager());
                                appPanel.setBounds(null);
                                postSetupRunnable = () -> { 
                                        Rectangle drawnSize = new Rectangle.Simple(appPanel);
                                        Dimension targetSize = new Dimension.Simple(drawnSize.getHeight()+4, drawnSize.getWidth());
                                        appPanel.setBounds(targetSize);
                                        //tApp.setCanvasSize(targetSize);
                                };
                        }
                        Rectangle drawnSizePre = new Rectangle.Simple(tApp.getMainApplicationPanel());

                    tApp.getMainApplicationPanel().add(
                            0, 1,
                            new HorizontalPanel().setHorizontalSpacer(0)
                                .add(new BorderedComponent(new TextComponent("CollapsibleSidePanel Test - expand, squeeze, collapse")))
                                .add(ThemePickerComponent.of(tApp).setBorder(true)));

                    // Content of the sidebar.
                    VerticalPanel sidebarContent = new VerticalPanel();
                    sidebarContent.setBorder(true);
                    sidebarContent.setTitle("Examples");

                    sidebarContent.add(new TextComponent("Documents"));
                    sidebarContent.add(new TextComponent("Components"));
                    sidebarContent.add(new TextComponent("Graphics"));
                    sidebarContent.add(new TextComponent("Themes"));
                    sidebarContent.add(new TextComponent("Settings"));

                    CollapsibleSidePanel sidebar =
                            new CollapsibleSidePanel(sidebarContent);
                                   // .setPreferredWidth(30)
                                   // .setMinimumWidth(12)
                                   // .setCollapsedWidth(2);

                    // Main content alongside the sidebar.
                    VerticalPanel mainContent = new VerticalPanel();
                    mainContent.setBorder(true);
                    mainContent.setTitle("Main Content");
                    mainContent.add(new TextComponent(
                            "This is the panel to the right of the sidebar."));
                    mainContent.add(new TextComponent(
                            "Watch its position as the sidebar changes width."));

                    HorizontalPanel layout =
                            new HorizontalPanel().setHorizontalSpacer(1);

                    layout.add(sidebar);
                    layout.add(mainContent);

                    tApp.getMainApplicationPanel().add(4, 2, layout);

                    // Controls to exercise the sidebar.
                    HorizontalPanel controls =
                            new HorizontalPanel().setHorizontalSpacer(1);

                    Button expand = new Button(Colors.BGre, "Expand", true);
                    Button collapse = new Button(Colors.BYel, "Collapse", true);
                    Button squeeze = new Button(Colors.BCya, "Squeeze", true);
                    Button narrow = new Button(Colors.BIPur, "Narrow", true);

                    controls.add(expand);
                    controls.add(collapse);
                    controls.add(squeeze);
                    controls.add(narrow);

                    tApp.getMainApplicationPanel().add(2, 2, controls);

                    expand.onChange(te -> sidebar.expand());

                    collapse.onChange(te -> sidebar.collapse());

                    squeeze.onChange(te -> sidebar.resize(
                            new Dimension.Simple(
                                    sidebar.getHeight(), 18)));

                    narrow.onChange(te -> sidebar.resize(
                            new Dimension.Simple(
                                    sidebar.getHeight(), 6)));

                    tApp.getEventBus().raiseEvent(RequestFocusEvent.fromSource(sidebar.getToggleButton()));


                    postSetupRunnable.run();
                //     Rectangle drawnSize = new Rectangle.Simple(tApp.getMainApplicationPanel());
                //     Dimension targetSize = new Dimension.Simple(drawnSize.getHeight()+1, drawnSize.getWidth());
                //     tApp.setCanvasSize(targetSize);
                //     if (drawnSizePre.isSameDimension(drawnSize)) {
                //         throw new NullPointerException("Same size this does not work! "+drawnSizePre);
                //     }

                })
                .start();
    }
}
