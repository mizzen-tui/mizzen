package mizzen.components.input.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mizzen.traverse.HasChildren;

/**
 * A simple tree node for building a tree by hand: a value and its children.
 * <pre>
 *     DefaultTreeNode&lt;String&gt; root = DefaultTreeNode.of("Shapes",
 *                                       DefaultTreeNode.of("Round", DefaultTreeNode.of("Circle"), DefaultTreeNode.of("Ellipse")),
 *                                       DefaultTreeNode.of("Square"));
 *     TreeView&lt;DefaultTreeNode&lt;String&gt;&gt; tree = new TreeView&lt;&gt;(root.asModel());
 * </pre>
 * Shown with its value's text (toString). For data that already is a tree (files, components ...)
 * write a TreeViewModel instead - it is only asked for children when a node is expanded.
 */
public class DefaultTreeNode<V> implements HasChildren<DefaultTreeNode<V>> {

    private final V value;
    private final List<DefaultTreeNode<V>> children = new ArrayList<DefaultTreeNode<V>>();

    public DefaultTreeNode(V value) {
        this.value = value;
    }

    @SafeVarargs
    public static <V> DefaultTreeNode<V> of(V value, DefaultTreeNode<V>... children) {
        DefaultTreeNode<V> node = new DefaultTreeNode<V>(value);
        for (DefaultTreeNode<V> child : children) {
            node.add(child);
        }
        return node;
    }

    public DefaultTreeNode<V> add(DefaultTreeNode<V> child) {
        children.add(child);
        return this;
    }

    public V getValue() {
        return value;
    }

    @Override
    public List<DefaultTreeNode<V>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /** this node as the root of a TreeViewModel */
    public TreeViewModel<DefaultTreeNode<V>> asModel() {
        DefaultTreeNode<V> root = this;
        return new TreeViewModel<DefaultTreeNode<V>>() {
            @Override
            public DefaultTreeNode<V> getRoot() {
                return root;
            }

            @Override
            public DefaultTreeNode<V> getChild(DefaultTreeNode<V> parent, int index) {
                return parent.children.get(index);
            }

            @Override
            public int getChildCount(DefaultTreeNode<V> parent) {
                return parent.children.size();
            }

            @Override
            public boolean isLeaf(DefaultTreeNode<V> node) {
                return node.children.isEmpty();
            }

            @Override
            public int getIndexOfChild(DefaultTreeNode<V> parent, DefaultTreeNode<V> child) {
                return parent.children.indexOf(child);
            }
        };
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
