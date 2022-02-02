package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.tree.TreeView;
import dtb.terminalIO.components.input.tree.TreeViewModel;
import dtb.terminalIO.components.input.tree.TreeViewModel.HasChildrenTreeViewModel;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class TreeViewTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        
        String root = "the root";
        String[] rootChildren = new String[] {"child1", "child2","child3", "child4"};
        String[] child1Children = new String[] {"grandchild1.1", "grandchild1.2"};
        String[] child2Children = new String[] {"grandchild2.1", "grandchild2.2"};
        String[] child3Children = new String[] {"grandchild3.1"};
        String[] grandchild31Children = new String[] {"grandchild3.1.1", "grandchild3.1.2","grandchild3.1.3"};
        String[] grandchild312Children = new String[] {"grandchild3.1.2.1"};
        String[] child4Children = new String[] {"grandchild4.1", "grandchild4.2"};
        String[] child42Children = new String[] {"grandchild4.2.1", "grandchild4.2.2"};
        
        TreeViewModel<String> model = new TreeViewModel<String>() {

            @Override
            public String getRoot() {
                return root;
            }

            private String[] getChildren(String parent) {
                if (parent == root) return rootChildren;
                if (parent == rootChildren[0]) return child1Children;
                if (parent == rootChildren[1]) return child2Children;
                if (parent == rootChildren[2]) return child3Children;
                if (parent == rootChildren[3]) return child4Children;
                if (parent == child3Children[0]) return grandchild31Children;
                if (parent == grandchild31Children[1]) return grandchild312Children;
                if (parent == child4Children[1]) return child42Children;
                return null;
            }
            
            @Override
            public String getChild(String parent, int index) {
                String[] children = getChildren(parent);
                if (children == null) return null;
                return children[index];
            }

            @Override
            public int getChildCount(String parent) {
                String[] children = getChildren(parent);
                if (children == null) return 0;
                return children.length;
            }

            @Override
            public boolean isLeaf(String node) {
                return getChildCount(node) == 0;
            }

            @Override
            public int getIndexOfChild(String parent, String child) {
                String[] children = getChildren(parent);
                if (children == null) return -1;
                for (int i = 0; i < children.length; i++) {
                    if (children[i].equals(child)) {
                        return i;
                    }
                }
                return -1;
            }
        };
        
        new TUIApp(args)
        .setCanvasSize(30, 100)
        .setEmptyChar(' ')
        .setMainApplicationPanel(new Composite().setBounds(40, 100).setEmptyChar(' ').setBorder(true))
        .confirmOnExit()
        .setOnReadyStart((tApp)->{ 

            Composite mainpanel = (Composite) tApp.getMainApplicationPanel();
            mainpanel.add(2,2,new TextComponent("This is a test of rendering simple TreeViews - CTRL-C to quit"));

            HorizontalPanel hp = new HorizontalPanel();

            TreeView<String>  tv = new TreeView<String>(Colors.IGre, new Dimension.Simple(20,35));
            tv.setModel( model);

            hp.add(new BorderedComponent(tv));

            mainpanel.add(4,2 , hp);

            TreeView<Component<?>>  tv2 = new TreeView<Component<?>>(Colors.IBlu, new Dimension.Simple(20,35), (node)->node.getClass().getSimpleName()  );
            tv2.setModel( new HasChildrenTreeViewModel<Component<?>>(mainpanel));

            hp.add( new ScrollableComponent(tv2.getColor(), tv2.getRectangle(), tv2));

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(
                    tApp.getMainApplicationPanel(), tApp.getEventBus(),
                    () -> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false));

            if (mainFocusManager != null) {
               tApp.getEventBus().appendEventHandler(mainFocusManager);
            }
            
            
        }).start();
    }

    
}
