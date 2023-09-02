package controller.xml;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

import controller.CompileMode;

public class PatternNode extends AbstractNode {

    private PatternNode parent = null;
    private final List<PatternNode> visibleChildren = new ArrayList<PatternNode>();
//    private final List<BilateralNode> totalChildren = new ArrayList<BilateralNode>();
    private final CompileMode mode;
    private final String name;
    private final Queue<PatternAttribute> patterns = new ArrayDeque<PatternAttribute>();;

//    private ViewType viewType = ViewType.VISIBLE;
    private boolean selected;
    private boolean disabled;
//    private boolean hidden;

    public PatternNode(CompileMode mode, String name, boolean selected, boolean disabled, /*boolean hidden,*/ PatternNode parent) {
        this(mode, name, selected || parent.selected, disabled || parent.disabled/*, hidden || parent.hidden*/);
    }

    public PatternNode(CompileMode mode, String name, boolean selected, boolean disabled/*, boolean hidden*/) {
        this.mode = mode;
        this.name = name;
        this.selected = selected;
        this.disabled = disabled;
//        this.hidden = hidden;
    }

    @Override
    public String toString() {
        if (this.mode.isDetailed()) {
            String hints = this.disabled ? "disabled" : "";
//            if (this.hidden) {
//                hints += (hints.isEmpty() ? "" : ", ") + "hidden";
//            }
            if (this.mode == CompileMode.DEBUG) {
                hints += (hints.isEmpty() ? "" : ", ") + (this.selected ? "selected" : "deselected");
            }
            return name + (hints.isEmpty() ? "" : String.format(" (%s)", hints));
        } else {
            return this.name;
        }
    }

//    @Override
//    public BilateralNode getVisibleView() {
//        this.viewType = ViewType.VISIBLE;
//        return this;
//    }
//
//    @Override
//    public BilateralNode getTotalView() {
//        this.viewType = ViewType.TOTAL;
//        return this;
//    }

    @Override
    protected List<PatternNode> getActiveChildren() {
        return /*viewType == ViewType.VISIBLE ? */this.visibleChildren/* : this.totalChildren*/;
    }

    public boolean isSelected() {
        return this.getSelectionType() == SelectionType.SELECTED;
    }

    public boolean isPartiallySelected() {
        return this.getSelectionType() == SelectionType.PARTIAL;
    }

    private SelectionType getSelectionType() {
        if (this.isLeaf()) {
            return this.selected ? SelectionType.SELECTED : SelectionType.UNSELECTED;
        } else {
            SelectionType previousType = null;
            for (PatternNode child : this.getActiveChildren()) {
                SelectionType type = child.getSelectionType();
                if (type == SelectionType.PARTIAL) {
                    return SelectionType.PARTIAL;
                } else if (previousType == null) {
                    previousType = type;
                } else if (type != previousType) {
                    return SelectionType.PARTIAL;
                } // else type == previousType then continue;
            }
            if (previousType == null) {
                throw new RuntimeException("Selection type cannot be null!");
            } else {
                return previousType;
            }
        }
    }

    public boolean isDisabled() {
        /*if (this.mode.isDetailed()) {
            return false;
        } else*/ if (this.isLeaf()) {
            return this.disabled;
        } else {
            for (PatternNode child : this.getActiveChildren()) {
                if (!child.isDisabled()) {
                    return false;
                }
            }
            return true;
        }
    }

//    public boolean isHidden() {
//        if (this.mode.isDetailed()) {
//            return false;
//        }
//        for (BilateralNode ancestor = this; ancestor != null; ancestor = ancestor.getParent()) {
//            if (ancestor.hidden) {
//                return true;
//            }
//        }
//        return false;
//    }

    public void addRegex(String regex, String... requiredSiblingNodes) {
        patterns.add(new PatternAttribute(regex, requiredSiblingNodes));
    }

    public boolean hasPatterns() {
        return !patterns.isEmpty();
    }

    public void setSelected(boolean selected) {
        if (/*this.isHidden() || */ !this.mode.isDetailed() && this.isDisabled()) {
            throw new UnsupportedOperationException(/*"Hidden or*/ "Disabled nodes must not change selection type");
        }
        if (this.isLeaf()) {
            this.selected = selected;
        } else {
            for (PatternNode child : this.getActiveChildren()) {
                if (!child.isDisabled()/* && !child.isHidden()*/) {
                    child.setSelected(selected);
                }
            }
        }
    }

    @Override
    public String getPathString() {
        StringBuilder sb = new StringBuilder(this.name + "]");
        for (PatternNode ancestor = this.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            sb.insert(0, ancestor.name + ", ");
        }
        sb.insert(0, "[");
        return sb.toString();
    }

    @Override
    public PatternNode getParent() {
        return this.parent;
    }

    @Override
    public void add(PatternNode child) {
//        this.totalChildren.add(child);
//        if (!child.isHidden()) {
            this.visibleChildren.add(child);
//        }
        child.parent = this;
    }

    public void resetAttributes() {
        this.selected = false;
        this.disabled = false;
    }

    private static boolean requirementsFulfilled(PatternNode node, PatternAttribute pa) throws SAXException {
        for (int i = 0; i < pa.requiredSiblingNodes.length; i++) {
            String requiredNodeName = pa.requiredSiblingNodes[i];
            if (node.name.equals(requiredNodeName)) {
                throw new SAXException("Required node must not be the node itself: " + node.getPathString());
            } else if (node.getParent() == null) {
                throw new SAXException("Root node cannot have required nodes");
            } else {
                PatternNode sibling = null;
                for (PatternNode pn : node.getParent().visibleChildren) {
                    if (pn.name.equals(requiredNodeName)) {
                        sibling = pn;
                        if (!pn.isSelected()) {
                            return false;
                        }
                    }
                }
                if (sibling == null) {
                    throw new SAXException("Sibling node " + requiredNodeName + " does not exist for node: " + node.getPathString());
                }
            }
        }
        return true;
    }

    private static void collectSelectedPatterns(Collection<Pattern> c, PatternNode node, StringBuilder sb) throws SAXException {
//        BilateralNode totalNode = node.getTotalView();
        if (node.isSelected() && node.hasPatterns()) {
            sb.append(newline + node.getPathString());
            for (PatternAttribute pa : node.getPatternAttributes()) {
                if (requirementsFulfilled(node, pa)) {
                    c.add(pa.pattern);
                    sb.append(newline + "        " + pa.pattern.pattern());
                }
            }
        } else {
            for (PatternNode child : node.getActiveChildren()) {
                collectSelectedPatterns(c, child, sb);
            }
        }
    }

    private static final String newline = System.getProperty("line.separator");

    @Override
    public Queue<Pattern> getAllSelectedPatterns() throws SAXException {
        Queue<Pattern> patterns = new ArrayDeque<Pattern>();
        StringBuilder sb = new StringBuilder("Selected Nodes:");
        collectSelectedPatterns(patterns, this, sb);
        LOGGER.config(sb.toString());
        return patterns;
    }

    public String getName() {
        return this.name;
    }

    public Queue<PatternAttribute> getPatternAttributes() {
        return this.patterns;
    }

    private enum SelectionType {
        SELECTED,
        UNSELECTED,
        PARTIAL;
    }

//    private enum ViewType {
//        VISIBLE,
//        TOTAL;
//    }
}
