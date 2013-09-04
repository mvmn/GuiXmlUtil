package x.mvmn.xmlworks.swing.components.xmlpanel;

import java.awt.BorderLayout;
import java.io.StringReader;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

// TODO: Refactor this abomination
public class XmlPanel extends JPanel {

	private static final long serialVersionUID = -2156646965977927038L;

	protected final JTextArea textArea = new JTextArea();
	protected final JTree domTree = new JTree(new String[0]);
	protected final Thread validatorThread;
	protected final DocumentBuilder domBuilder;

	public XmlPanel() {
		this("");
	}

	public XmlPanel(String originalStr) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);

		try {
			domBuilder = factory.newDocumentBuilder();
			domBuilder.setErrorHandler(new ErrorHandler() {
				public void warning(SAXParseException e) throws SAXException {
				}

				public void error(SAXParseException e) throws SAXException {
					throw e;
				}

				public void fatalError(SAXParseException e) throws SAXException {
					throw e;
				}
			});
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}

		textArea.setText(originalStr);
		ValidatorThread validatorThreadLogic = new ValidatorThread();
		validatorThreadLogic.runOnce();
		validatorThreadLogic.runOnce();
		domTree.setRootVisible(true);
		this.setLayout(new BorderLayout());
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(textArea), new JScrollPane(domTree));
		splitPane.setResizeWeight(0.6);
		splitPane.setDividerLocation(0.6);
		this.add(splitPane, BorderLayout.CENTER);

		validatorThread = new Thread(validatorThreadLogic);
		validatorThread.setDaemon(true);
		validatorThread.start();
	}

	protected class DomTreeModel implements TreeModel {

		private final Document document;

		public DomTreeModel(final Document document) {
			this.document = document;
		}

		public Object getRoot() {
			return new NodeWrapper(document.getFirstChild());
		}

		public Object getChild(Object parent, int index) {
			NamedNodeMap attributes = ((Node) parent).getAttributes();
			if (index < attributes.getLength()) {
				return new NodeWrapper(attributes.item(index));
			}
			return new NodeWrapper(((Node) parent).getChildNodes().item(index - attributes.getLength()));
		}

		public int getChildCount(Object parent) {
			return ((Node) parent).getChildNodes().getLength() + ((Node) parent).getAttributes().getLength();
		}

		public boolean isLeaf(Object node) {
			return ((Node) node).getNodeType() != Node.ELEMENT_NODE;
		}

		public int getIndexOfChild(Object parent, Object child) {
			int index = -1;
			for (int i = 0; i < ((Node) parent).getAttributes().getLength(); i++) {
				Node node = ((Node) parent).getAttributes().item(index);
				if (node == child) {
					index = i;
					break;
				}
			}
			for (int i = 0; i < ((Node) parent).getChildNodes().getLength(); i++) {
				Node node = ((Node) parent).getChildNodes().item(i);
				if (node == child) {
					index = i;
					break;
				}
			}
			return index;
		}

		@Override
		public void valueForPathChanged(TreePath path, Object newValue) {
		}

		@Override
		public void addTreeModelListener(TreeModelListener l) {
		}

		@Override
		public void removeTreeModelListener(TreeModelListener l) {
		}
	}

	protected class ValidatorThread implements Runnable {
		String displayedVal = "";
		String oldVal = null;

		public void runOnce() {
			String currentVal = textArea.getText();
			try {
				if (currentVal.equals(oldVal)) {
					if (!displayedVal.equals(currentVal)) {
						Document model = validateAndParse(currentVal);
						if (model != null) {
							domTree.setModel(new DomTreeModel(model));
							for (int i = 0; i < domTree.getRowCount(); i++) {
								domTree.expandRow(i);
							}
							displayedVal = currentVal;
						}
					}
				}
			} catch (Exception e) {
			}
			oldVal = currentVal;
		}

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				Thread.yield();
				runOnce();
			}
		}
	}

	protected class NodeWrapper implements Node {

		private final Node originalNode;

		public NodeWrapper(final Node node) {
			this.originalNode = node;
		}

		public String getNodeName() {
			return originalNode.getNodeName();
		}

		public String getNodeValue() throws DOMException {
			return originalNode.getNodeValue();
		}

		public void setNodeValue(String nodeValue) throws DOMException {
			originalNode.setNodeValue(nodeValue);
		}

		public short getNodeType() {
			return originalNode.getNodeType();
		}

		public Node getParentNode() {
			return originalNode.getParentNode();
		}

		public NodeList getChildNodes() {
			return originalNode.getChildNodes();
		}

		public Node getFirstChild() {
			return originalNode.getFirstChild();
		}

		public Node getLastChild() {
			return originalNode.getLastChild();
		}

		public Node getPreviousSibling() {
			return originalNode.getPreviousSibling();
		}

		public Node getNextSibling() {
			return originalNode.getNextSibling();
		}

		public NamedNodeMap getAttributes() {
			return originalNode.getAttributes();
		}

		public Document getOwnerDocument() {
			return originalNode.getOwnerDocument();
		}

		public Node insertBefore(Node newChild, Node refChild) throws DOMException {
			return originalNode.insertBefore(newChild, refChild);
		}

		public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
			return originalNode.replaceChild(newChild, oldChild);
		}

		public Node removeChild(Node oldChild) throws DOMException {
			return originalNode.removeChild(oldChild);
		}

		public Node appendChild(Node newChild) throws DOMException {
			return originalNode.appendChild(newChild);
		}

		public boolean hasChildNodes() {
			return originalNode.hasChildNodes();
		}

		public Node cloneNode(boolean deep) {
			return originalNode.cloneNode(deep);
		}

		public void normalize() {
			originalNode.normalize();
		}

		public boolean isSupported(String feature, String version) {
			return originalNode.isSupported(feature, version);
		}

		public String getNamespaceURI() {
			return originalNode.getNamespaceURI();
		}

		public String getPrefix() {
			return originalNode.getPrefix();
		}

		public void setPrefix(String prefix) throws DOMException {
			originalNode.setPrefix(prefix);
		}

		public String getLocalName() {
			return originalNode.getLocalName();
		}

		public boolean hasAttributes() {
			return originalNode.hasAttributes();
		}

		public String getBaseURI() {
			return originalNode.getBaseURI();
		}

		public short compareDocumentPosition(Node other) throws DOMException {
			return originalNode.compareDocumentPosition(other);
		}

		public String getTextContent() throws DOMException {
			return originalNode.getTextContent();
		}

		public void setTextContent(String textContent) throws DOMException {
			originalNode.setTextContent(textContent);
		}

		public boolean isSameNode(Node other) {
			return originalNode.isSameNode(other);
		}

		public String lookupPrefix(String namespaceURI) {
			return originalNode.lookupPrefix(namespaceURI);
		}

		public boolean isDefaultNamespace(String namespaceURI) {
			return originalNode.isDefaultNamespace(namespaceURI);
		}

		public String lookupNamespaceURI(String prefix) {
			return originalNode.lookupNamespaceURI(prefix);
		}

		public boolean isEqualNode(Node arg) {
			return originalNode.isEqualNode(arg);
		}

		public Object getFeature(String feature, String version) {
			return originalNode.getFeature(feature, version);
		}

		public Object setUserData(String key, Object data, UserDataHandler handler) {
			return originalNode.setUserData(key, data, handler);
		}

		public Object getUserData(String key) {
			return originalNode.getUserData(key);
		}

		public String toString() {
			return nodeToString(this);
		}

	}

	protected static String nodeToString(Node node) {
		String result;
		switch (node.getNodeType()) {
			case Node.ATTRIBUTE_NODE:
				result = "@" + node.getNodeName() + "=" + node.getNodeValue();
			break;
			case Node.TEXT_NODE:
				result = "[" + node.getNodeValue() + "]";
			break;
			case Node.CDATA_SECTION_NODE:
				result = "CDATA: [" + node.getNodeValue() + "]";
			break;
			case Node.COMMENT_NODE:
				result = "Comment: [" + node.getNodeValue() + "]";
			break;
			case Node.DOCUMENT_FRAGMENT_NODE:
				result = "Doc. fragment: [" + node.getNodeName() + "]";
			break;
			case Node.DOCUMENT_NODE:
				result = "Document: [" + node.getNodeName() + "]";
			break;
			case Node.DOCUMENT_TYPE_NODE:
				result = "Doctype: [" + node.getNodeName() + "]";
			break;
			case Node.ELEMENT_NODE:
				result = "<" + node.getNodeName() + ">";
			break;
			case Node.ENTITY_NODE:
				result = "Entity: [" + node.getNodeName() + "]";
			break;
			case Node.ENTITY_REFERENCE_NODE:
				result = "Entity ref.: [" + node.getNodeName() + "]";
			break;
			case Node.NOTATION_NODE:
				result = "Notation: [" + node.getNodeName() + "]";
			break;
			case Node.PROCESSING_INSTRUCTION_NODE:
				result = "Proc.Instr.: [" + node.getNodeValue() + "]";
			break;
			default:
				result = node.toString();
		}

		return result;
	}

	public Document validateAndParse(String xml) {
		Document result = null;
		try {
			result = domBuilder.parse(new InputSource(new StringReader(xml)));
		} catch (Exception e) {
		}
		return result;
	}

	public String getText() {
		return textArea.getText();
	}

	public void setTabSize(int tabSize) {
		textArea.setTabSize(tabSize);
	}

	public void setText(String text) {
		textArea.setText(text);
	}

}
