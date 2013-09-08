package controller;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

import javax.swing.tree.TreeNode;

public abstract class AbstractNode implements TreeNode {
    
    protected abstract List<PatternNode> getActiveChildren();
    
//    public abstract BilateralNode getVisibleView();
//    
//    public abstract BilateralNode getTotalView();
    
    public abstract void add(PatternNode child);

    @Override
    public abstract AbstractNode getParent();
    
    public abstract Queue<Pattern> getAllSelectedPatterns();

    public abstract String getPathString();
    
    @Override
    public Enumeration<? extends AbstractNode> children() {
        return Collections.enumeration(this.getActiveChildren());
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return this.getActiveChildren().get(childIndex);
    }

    @Override
    public int getChildCount() {
        return this.getActiveChildren().size();
    }

    @Override
    public int getIndex(TreeNode node) {
        int i = 0;
        for (AbstractNode n : this.getActiveChildren()) {
            if (n == node) {
                return i; 
            }
            i++;
        }
        return -1;
    }

    @Override
    public boolean isLeaf() {
        return this.getActiveChildren().isEmpty();
    }

}
