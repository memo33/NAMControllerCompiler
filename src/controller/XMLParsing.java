package controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.utils.DefaultErrorHandler;

/**
 * Utility class for parsing the XML and DTD files, containing the IID structure
 * specifications.
 * @author memo
 * 
 * based on http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
 * by mkyong
 */
public class XMLParsing {

	/**
	 * Builds a JTree of MyNodes, parsed from the XML file of iid_tree structure.
	 * @param xmlFile the XML file containing the iid_tree structure.
	 * @return the JTree to be displayed in the GUI.
	 * @throws ParserConfigurationException
	 * @throws SAXException if XML is not well-formed and possibly other cases.
	 * @throws IOException
	 * @throws PatternSyntaxException
	 */
	public static JTree buildJTreeFromXML(File xmlFile) throws ParserConfigurationException, SAXException, IOException, PatternSyntaxException {	
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setValidating(true);

		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		dBuilder.setErrorHandler(new DefaultErrorHandler());
		Document doc = dBuilder.parse(xmlFile);
		doc.getDocumentElement().normalize();

		NodeList nodeList = doc.getChildNodes();
		Queue<MyNode> children = getMyNodes(nodeList, null);
		MyNode rootNode = children.peek();
		return new JTree(rootNode);
	}

	/**
	 * Recursive parsing and building of the tree.
	 * @param nodeList
	 * @param parent
	 * @return a queue of child nodes.
	 * @throws SAXException if XML is not well-formed and possibly other cases.
	 */
	private static Queue<MyNode> getMyNodes(NodeList nodeList, MyNode parent) throws SAXException {
		Queue<MyNode> queue = new ArrayDeque<MyNode>();
		for (int count = 0; count < nodeList.getLength(); count++) {
			Node tempNode = nodeList.item(count);
			if (tempNode.getNodeType() == Node.ELEMENT_NODE) { // make sure it's element node.

				String nodeName = "All Networks";
				String s = tempNode.getNodeName();
				if (s.equals("node")) {
					nodeName = tempNode.getAttributes().getNamedItem("name").getNodeValue();
				}
				if (s.equals("node") || s.equals("iid_tree")) {
					MyNode myNode = new MyNode(nodeName);
					if (tempNode.hasChildNodes()) {
						Queue<MyNode> children = getMyNodes(tempNode.getChildNodes(), myNode);
						for (MyNode child : children)
							myNode.add(child);
					}
					queue.add(myNode);
				} else if (s.equals("regex")) {
					String regexValue = tempNode.getAttributes().getNamedItem("value").getNodeValue();
					parent.addRegex(regexValue);
				} else {
					throw new SAXException("Invalid node name in XML file: " + tempNode.getNodeName());
				}
			}
		}
		return queue;
	}

    /**
     * A Subclass of a TreeNode to include the queue of regex-patterns into the node.
     * @author memo
     */
    @SuppressWarnings("serial")
    public static class MyNode extends DefaultMutableTreeNode implements Iterable<Pattern> {
    	private Queue<Pattern> patterns = null;
    
    	public MyNode(String name) {
    		super(name);
    		patterns = new ArrayDeque<Pattern>();
    	}
    	
    	public void addRegex(String regex) {
    		patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
    	}
    	
    	public boolean hasPatterns() {
    		return !patterns.isEmpty();
    	}
    	
    	@Override
    	public Iterator<Pattern> iterator() {
    		return patterns.iterator();
    	}
    }
}
