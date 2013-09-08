package view.checkboxtree;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.RowMapper;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import controller.XMLParsing.MyNode;

/**
 * @author Santhosh Kumar T - santhosh@in.fiorano.com 
 * http://www.jroller.com/santhosh/date/20050610
 * 
 * modified by memo
 */
@SuppressWarnings("serial")
public class MyCheckTreeSelectionModel/* extends DefaultTreeSelectionModel*/ implements TreeSelectionModel { 
    private final MyNode rootNode;
    private List<TreeSelectionListener> listeners = new ArrayList<TreeSelectionListener>();
 
    public MyCheckTreeSelectionModel(TreeModel model){ 
        this.rootNode = (MyNode) model.getRoot();
//        setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    } 
    
    public boolean isPartiallySelected(TreePath path) {
        MyNode node = (MyNode) path.getLastPathComponent();
        return node.isPartiallySelected();
    }
    
    @Override
    public void addSelectionPath(TreePath path) {
        MyNode node = (MyNode) path.getLastPathComponent();
        if (!node.isDisabled()) {
            node.setSelected(true);
        }
    }

    @Override
    public void addSelectionPaths(TreePath[] paths) {
        for (TreePath p : paths) {
            addSelectionPath(p);
        }
    }
    
    @Override
    public void removeSelectionPath(TreePath path) {
        final MyNode node = (MyNode) path.getLastPathComponent();
        if (!node.isDisabled()) {
            node.setSelected(false);
        }
    }

    @Override
    public void removeSelectionPaths(TreePath[] paths) {
        for (TreePath p : paths) {
            removeSelectionPath(p);
        }
    }
    
    @Override
    public TreePath getSelectionPath() {
        throw new UnsupportedOperationException("Not implemented.");
    }
    
    @Override
    public TreePath[] getSelectionPaths() {
        throw new UnsupportedOperationException("Not implemented.");
//        ArrayList<TreePath> selectedPaths = new ArrayList<TreePath>();
//        collectSelectedNodes(selectedPaths, rootNode);
//        return selectedPaths.toArray(new TreePath[selectedPaths.size()]);
    }
    
    private void collectSelectedNodes(Collection<TreePath> c, MyNode parent) {
        if (parent.isSelected()) {
            c.add(new TreePath(parent.getPath()));
        } else {
            Enumeration<MyNode> children = parent.children();
            while (children.hasMoreElements()) {
                collectSelectedNodes(c, children.nextElement());
            }
        }
    }

    @Override
    public void addTreeSelectionListener(TreeSelectionListener x) {
        this.listeners.add(x);
    }
    
    @Override
    public void removeTreeSelectionListener(TreeSelectionListener x) {
        listeners.remove(x);
    }
    
    @Override
    public void setSelectionPath(TreePath path) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void setSelectionPaths(TreePath[] paths) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void clearSelection() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public TreePath getLeadSelectionPath() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public int getLeadSelectionRow() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public int getMaxSelectionRow() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public int getMinSelectionRow() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public RowMapper getRowMapper() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public int getSelectionCount() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public int getSelectionMode() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public int[] getSelectionRows() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public boolean isPathSelected(TreePath path) {
        return ((MyNode) path.getLastPathComponent()).isSelected();
    }

    @Override
    public boolean isRowSelected(int row) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public boolean isSelectionEmpty() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void resetRowSelection() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void setRowMapper(RowMapper newMapper) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void setSelectionMode(int mode) {
        throw new UnsupportedOperationException("Not implemented.");
    }
}