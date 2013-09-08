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
}
