package view.checkboxtree;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import controller.xml.AbstractNode;
import controller.xml.PatternNode;

/**
 * @author Santhosh Kumar T - santhosh@in.fiorano.com 
 * http://www.jroller.com/santhosh/date/20050610
 * 
 * modified by memo
 */
public class MyCheckTreeManager extends MouseAdapter implements TreeSelectionListener{ 
    private MyCheckTreeSelectionModel selectionModel; 
    private JTree tree = new JTree(); 
    int hotspot = new JCheckBox().getPreferredSize().width; 
 
    public MyCheckTreeManager(JTree tree, boolean enableDisabledButtons){ 
        this.tree = tree; 
        selectionModel = new MyCheckTreeSelectionModel(/*tree.getModel(), */ enableDisabledButtons);
        tree.setCellRenderer(new MyCheckTreeCellRenderer(tree.getCellRenderer(), selectionModel, enableDisabledButtons)); 
        tree.addMouseListener(this); 
        selectionModel.addTreeSelectionListener(this); 
    }
    
    @Override
    public void mouseClicked(MouseEvent me) {
        TreePath path = tree.getPathForLocation(me.getX(), me.getY()); 
        if (path != null && me.getX() > tree.getPathBounds(path).x + hotspot) {
            if (me.getClickCount() > 1 && me.getClickCount() % 2 == 0) {
                if (tree.isCollapsed(path)) {
                    tree.expandPath(path);
                } else {
                    tree.collapsePath(path);
                }
            }
        }
    }
    
    @Override
    public void mousePressed(MouseEvent me){ 
        TreePath path = tree.getPathForLocation(me.getX(), me.getY()); 
        if(path==null) 
            return; 
        if(me.getX()>tree.getPathBounds(path).x+hotspot)
            return;
 
        boolean selected = selectionModel.isPathSelected(path);
        boolean partiallySelected = selectionModel.isPartiallySelected(path);
        selectionModel.removeTreeSelectionListener(this);
 
        try{ 
            if(selected) {
                selectionModel.removeSelectionPath(path);
            } else if (partiallySelected) {
                if (shouldAdd((PatternNode) path.getLastPathComponent())) {
                    selectionModel.addSelectionPath(path);
                } else {
                    selectionModel.removeSelectionPath(path);
                }
            } else { 
                selectionModel.addSelectionPath(path);
            }
        } finally{ 
            selectionModel.addTreeSelectionListener(this); 
            tree.treeDidChange();
        } 
    }
    
    private static boolean shouldAdd(PatternNode parent) {
        Enumeration<? extends AbstractNode> children = parent.children();
        while (children.hasMoreElements()) {
            PatternNode child = (PatternNode) children.nextElement();
            if (!child.isDisabled() && !child.isSelected() && !child.isPartiallySelected()) {
                return true;
            } else if (!child.isDisabled() && child.isPartiallySelected()) {
                if(shouldAdd(child)) {
                    return true;
                }
            }
        }
        return false;
    }
 
    public MyCheckTreeSelectionModel getSelectionModel(){ 
        return selectionModel; 
    } 
 
    public void valueChanged(TreeSelectionEvent e){ 
        tree.treeDidChange(); 
    } 
}
