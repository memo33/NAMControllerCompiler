package controller;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JTree;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.utils.DefaultErrorHandler;

import controller.AbstractCompiler.Mode;

/**
 * Utility class for parsing the XML and DTD files, containing the IID structure
 * specifications.
 * @author memo
 * 
 * based on http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
 * by mkyong
 */
public class XMLParsing {
    
    private static String SYSTEM_VALUE = "RUL2_IID_structure.dtd";
    
    private static String KEY_NODE = "node";
    private static String KEY_IIDTREE = "iid_tree";
    private static String KEY_REGEX = "regex";
    private static String KEY_NAME = "name";
    private static String KEY_VALUE = "value";
    private static String KEY_SELECTED = "selected";
    private static String KEY_DISABLED = "disabled";
//    private static String KEY_HIDDEN = "hidden";
    
    public static void writeXMLfromJTree(JTree tree, File xmlFile) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setValidating(true);
        
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();
        
        Element rootElement = doc.createElement(KEY_IIDTREE);
        doc.appendChild(rootElement);
        appendNodes((PatternNode) tree.getModel().getRoot(), doc, rootElement);
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, SYSTEM_VALUE);
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(xmlFile);
        
        transformer.transform(source, result);
        LOGGER.info("XML file saved: " + xmlFile);
    }

    private static void appendNodes(PatternNode parentNode, Document doc, Element parentElement) {
        if (parentNode.isSelected()) {
            parentElement.setAttribute(KEY_SELECTED, "true");
        }
        if (parentNode.isDisabled()) {
            parentElement.setAttribute(KEY_DISABLED, "true");
        }
        for (Pattern p : parentNode.getPatterns()) {
            Element regexElement = doc.createElement(KEY_REGEX);
            regexElement.setAttribute(KEY_VALUE, p.pattern());
            parentElement.appendChild(regexElement);
        }
        for (PatternNode childNode : parentNode.getActiveChildren()) {
            Element childElement = doc.createElement(KEY_NODE);
            childElement.setAttribute(KEY_NAME, childNode.getName());
            parentElement.appendChild(childElement);
            appendNodes(childNode, doc, childElement);
        }
    }
    
	/**
	 * Builds a JTree of MyNodes, parsed from the XML file of iid_tree structure.
	 * @param xmlFile the XML file containing the iid_tree structure.
	 * @return the JTree to be displayed in the GUI.
	 * @throws ParserConfigurationException
	 * @throws SAXException if XML is not well-formed and possibly other cases.
	 * @throws IOException
	 * @throws PatternSyntaxException
	 */
	public static JTree buildJTreeFromXML(Mode mode, File xmlFile) throws ParserConfigurationException, SAXException, IOException, PatternSyntaxException {	
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setValidating(true);

		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		dBuilder.setErrorHandler(new DefaultErrorHandler());
		Document doc = dBuilder.parse(xmlFile);
		doc.getDocumentElement().normalize();

		NodeList nodeList = doc.getChildNodes();
		Queue<PatternNode> children = getMyNodes(mode, nodeList, null);
		PatternNode rootNode = children.peek();
		return new JTree(rootNode);
	}
	
	/**
	 * Recursive parsing and building of the tree.
	 * @param nodeList
	 * @param parent
	 * @return a queue of child nodes.
	 * @throws SAXException if XML is not well-formed and possibly other cases.
	 */
	private static Queue<PatternNode> getMyNodes(Mode mode, NodeList nodeList, PatternNode parent) throws SAXException {
		Queue<PatternNode> queue = new LinkedList<PatternNode>();
		for (int count = 0; count < nodeList.getLength(); count++) {
			Node tempNode = nodeList.item(count);
			if (tempNode.getNodeType() == Node.ELEMENT_NODE) { // make sure it's element node.

				String nodeName = "All Networks";
				boolean disabled = false, selected = false/*, hidden = false*/;
				String s = tempNode.getNodeName();
				if (s.equals(KEY_NODE)) {
					nodeName = tempNode.getAttributes().getNamedItem(KEY_NAME).getNodeValue();
					selected |= tempNode.getAttributes().getNamedItem(KEY_SELECTED).getNodeValue().equals("true");
					disabled |= tempNode.getAttributes().getNamedItem(KEY_DISABLED).getNodeValue().equals("true");
//					hidden |= tempNode.getAttributes().getNamedItem(KEY_HIDDEN).getNodeValue().equals("true");
				}
				if (s.equals(KEY_NODE) || s.equals(KEY_IIDTREE)) {
					PatternNode myNode = parent == null ?
					        new PatternNode(mode, nodeName, selected, disabled/*, hidden*/) :
					            new PatternNode(mode, nodeName, selected, disabled/*, hidden*/, parent);
					if (tempNode.hasChildNodes()) {
						Queue<PatternNode> children = getMyNodes(mode, tempNode.getChildNodes(), myNode);
						if (children.size() != 0) {
						    // store attributes in leaf nodes only
						    myNode.resetAttributes();
//						    myNode.disabled = false;
//						    myNode.selected = false;
//						    myNode.hidden = false;
						}
						for (PatternNode child : children)
							myNode.add(child);
					}
					queue.add(myNode);
				} else if (s.equals(KEY_REGEX)) {
					String regexValue = tempNode.getAttributes().getNamedItem(KEY_VALUE).getNodeValue();
					parent.addRegex(regexValue);
				} else {
					throw new SAXException("Invalid node name in XML file: " + tempNode.getNodeName());
				}
			}
		}
		return queue;
	}

//    /**
//     * A Subclass of a TreeNode to include the queue of regex-patterns into the node.
//     * @author memo
//     */
//    @SuppressWarnings("serial")
//    public static class MyNode extends DefaultMutableTreeNode implements Iterable<Pattern> {
//    	private Queue<Pattern> patterns = null;
//    	private boolean selected;
//    	private boolean enabled;
//    	private boolean hidden;
//    
//    	public MyNode(String name, boolean selected, boolean enabled, boolean hidden) {
//    		super(name);
//    		patterns = new ArrayDeque<Pattern>();
//    		this.selected = selected;
//    		this.enabled = enabled;
//    		this.hidden = hidden;
//    	}
//    	
//    	public void addRegex(String regex) {
//    		patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
//    	}
//    	
//    	public boolean hasPatterns() {
//    		return !patterns.isEmpty();
//    	}
//    	
//    	@Override
//    	public Iterator<Pattern> iterator() {
//    		return patterns.iterator();
//    	}
//    	
//    	public boolean isSelected() {
//    	    return this.selected;
//    	}
//    	
//    	public boolean isEnabled() {
//    	    return this.enabled;
//    	}
//    	
//    	public boolean isHidden() {
//    	    return this.hidden;
//    	}
//    }
    
//	public static class MyNode extends DefaultMutableTreeNode implements TreeNode {
//
//        private List<MyNode> visibleChildren = new ArrayList<MyNode>();
//        private List<MyNode> allChildren = new ArrayList<MyNode>();
//        private MyNode parent;
//        private final String name;
//        private final Mode mode;
//        
//        private final Queue<Pattern> patterns;
//        private boolean selected;
//        private boolean disabled;
//        private boolean hidden;
//    
//
//        public MyNode(Mode mode, String name, boolean selected, boolean disabled, boolean hidden) {
//            this.mode = mode;
//            if (mode.isDetailed()) {
//                String hints = disabled ? "disabled" : "";
//                if (hidden) {
//                    hints += (hints.isEmpty() ? "" : ", ") + "hidden";
//                }
//                this.name = name + (hints.isEmpty() ? "" : String.format(" (%s)", hints));
//            } else {
//                this.name = name;
//            }
//            patterns = new ArrayDeque<Pattern>();
//            this.selected = selected;
//            this.disabled = disabled;
//            this.hidden = hidden;
//        }
//        
//        public void add(MyNode child) {
//            this.allChildren.add(child);
//            if (!child.isHidden() || this.mode.isDetailed()) {
//                this.visibleChildren.add(child);
//            }
//            child.parent = this;
//        }
//        
//        @Override
//        public String toString() {
//            return name + (selected ? " (selected)" : " (deselected)");
//        }
//        
//        @Override
//        public Enumeration<MyNode> children() {
//            return Collections.enumeration(visibleChildren);
//        }
//        
//        public Collection<MyNode> getChildren() {
//            return visibleChildren;
//        }
//
//        @Override
//        public boolean getAllowsChildren() {
//            return true;
//        }
//
//        @Override
//        public TreeNode getChildAt(int childIndex) {
//            return visibleChildren.get(childIndex);
//        }
//
//        @Override
//        public int getChildCount() {
//            return visibleChildren.size();
//        }
//
//        @Override
//        public int getIndex(TreeNode node) {
//            int i = 0;
//            for (TreeNode n : visibleChildren) {
//                if (n == node) {
//                    return i;
//                }
//            }
//            return -1;
//        }
//
//        @Override
//        public MyNode getParent() {
//            return parent;
//        }
//
//        @Override
//        public boolean isLeaf() {
//            return visibleChildren.size() == 0;
//        }
//        
//        private void addRegex(String regex) {
//            patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
//        }
//        
//        private boolean hasPatterns() {
//            return !patterns.isEmpty();
//        }
//        
////        @Override
////        public Iterator<Pattern> iterator() {
////            return patterns.iterator();
////        }
//        
//        public boolean isSelected() {
//            if (this.isLeaf()) {
//                return this.selected;
//            } else {
//                for (MyNode child : getChildren()) {
//                    if (!child.isSelected()) {
//                        return false;
//                    }
//                }
//                return true;
//            }
//        }
//        
//        public boolean isPartiallySelected() {
//            if (this.isLeaf()) {
//                return false;
//            } else {
////                for (MyNode child : getChildren()) {
////                    if (child.isPartiallySelected()) {
////                        return true;
////                    }
////                }
//                Collection<MyNode> c = new ArrayDeque<MyNode>();
//                collectLeaves(c, this);
//                boolean selectedFound = false, unselectedFound = false;
//                for (MyNode n : c) {
//                    if (!selectedFound && n.selected) {
//                        selectedFound = true;
//                    }
//                    if (!unselectedFound && !n.selected) {
//                        unselectedFound = true;
//                    }
//                    if (selectedFound && unselectedFound) {
//                        return true;
//                    }
//                }
//                return false;
//            }
//        }
//        
//        private void collectLeaves(Collection<MyNode> c, MyNode node) {
//            if (node.isLeaf()) {
//                c.add(node);
//            } else {
//                for (MyNode child : node.getChildren()) {
//                    collectLeaves(c, child);
//                }
//            }
//        }
//        
//        public boolean isDisabled() {
//            if (this.mode.isDetailed()) {
//                return false;
//            } else if (this.isLeaf()) {
//                return this.disabled;
//            } else {
//                for (MyNode child : getChildren()) {
//                    if (!child.isDisabled()) {
//                        return false;
//                    }
//                }
//                return true;
//            }
//        }
//        
//        public boolean isHidden() {
//            if (this.mode.isDetailed()) {
//                return false;
//            }
//            for (MyNode ancestor = this; ancestor != null; ancestor = ancestor.parent) {
//                if (ancestor.hidden) {
//                    return true;
//                }
//            }
//            return false;
//        }
//
//        public void setSelected(boolean selected) {
//            if (this.isHidden() || this.isDisabled()) {
//                throw new UnsupportedOperationException("Hidden or disabled nodes must not change selection type");
//            }
//            if (this.isLeaf()) {
//                this.selected = selected;
//            } else {
//                for (MyNode child : getChildren()) {
//                    if (!child.isDisabled()) {
//                        child.setSelected(selected);
//                    }
//                }
//            }
//        }
//        
//        private boolean isSelectedFully() {
//            if (this.allChildren.size() == 0) {
//                return this.selected;
//            } else {
//                for (MyNode child : allChildren) {
//                    if (!child.isSelectedFully()) {
//                        return false;
//                    }
//                }
//                return true;
//            }
//        }
//        
//        private void collectSelectedPatterns(Collection<Pattern> c, MyNode node, StringBuilder sb) {
//            if (node.isSelectedFully() && node.hasPatterns()) {
//                sb.append(newline + new TreePath(node.getPath()));
//                c.addAll(node.patterns);
//            } else {
//                for (MyNode child : node.allChildren) {
//                    collectSelectedPatterns(c, child, sb);
//                }
//            }
//        }
//        
//        private static final String newline = System.getProperty("line.separator");
//        
//        public Queue<Pattern> getAllSelectedPatterns() {
//            Queue<Pattern> patterns = new ArrayDeque<Pattern>();
//            StringBuilder sb = new StringBuilder("Selected Nodes:");
//            collectSelectedPatterns(patterns, this, sb);
//            LOGGER.config(sb.toString());
//            return patterns;
//        }
//    }
	
//	private static void printNode(MyNode node, int indent) {
//	    System.out.printf("%" + indent + "s %s %s %s %s%n", "", node.name, node.selected ? "selected" : "deselected", node.disabled ? "disabled" : "", node.hidden ? "hidden" : "");
//	    indent += 2;
//	    for (Pattern p : node.patterns) {
//	        System.out.printf("%" + indent + "s %s%n", "", p);
//	    }
//	    for (MyNode child : node.allChildren) {
//	        printNode(child, indent);
//	    }
//	}
}
