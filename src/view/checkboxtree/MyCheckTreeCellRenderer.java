package view.checkboxtree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractButton;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import controller.PatternNode;

/**
 * @author Santhosh Kumar T - santhosh@in.fiorano.com 
 * http://www.jroller.com/santhosh/date/20050610
 * 
 * modified by memo
 */
@SuppressWarnings("serial")
public class MyCheckTreeCellRenderer extends JPanel implements TreeCellRenderer{
    
    private static Map<Icon, Icon> disabledIconsMap = new ConcurrentHashMap<Icon, Icon>();

    private MyCheckTreeSelectionModel selectionModel; 
    private TreeCellRenderer delegate; 
    private TristateCheckBox checkBox = new TristateCheckBox(null);
    private JRadioButton radioButton = new JRadioButton();
 
    public MyCheckTreeCellRenderer(TreeCellRenderer delegate, MyCheckTreeSelectionModel selectionModel){ 
        this.delegate = delegate; 
        this.selectionModel = selectionModel; 
        setLayout(new BorderLayout()); 
        setOpaque(false); 
        checkBox.setOpaque(false);
        radioButton.setSelected(true);
        radioButton.setOpaque(false);
    } 
 
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus){
        Component renderer = delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        PatternNode visibleNode = ((PatternNode) value);
 
        AbstractButton button = checkBox;
        TreePath path = tree.getPathForRow(row); 
        if(path!=null) {
        	TristateButtonModel tristateModel = checkBox.getTristateModel();
            if(selectionModel.isPathSelected(path)) {
            	tristateModel.setSelected(true);
            } else if (selectionModel.isPartiallySelected(path)) {
            	tristateModel.setIndeterminate();
            	button = radioButton;
            } else {
                tristateModel.setSelected(false); 
            }
        } 
        removeAll();
        
        button.setEnabled(!visibleNode.isDisabled());
        renderer.setEnabled(!visibleNode.isDisabled());
        if (visibleNode.isDisabled()) {
            JLabel label = (JLabel) renderer;
            Icon icon = label.getIcon();
            Icon disabledIcon;
            if (disabledIconsMap.containsKey(icon)) {
                disabledIcon = disabledIconsMap.get(icon);
            } else {
                Image img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                icon.paintIcon(new JPanel(), img.getGraphics(), 0, 0);
                disabledIcon = new ImageIcon(GrayFilter.createDisabledImage(img));
                disabledIconsMap.put(icon, disabledIcon);
            }
            label.setDisabledIcon(disabledIcon);
        }
        add(button, BorderLayout.WEST);
        add(renderer, BorderLayout.CENTER);
        return this; 
    } 
} 