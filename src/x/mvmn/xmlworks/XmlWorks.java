package x.mvmn.xmlworks;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlWorks implements ActionListener {

	private final JFrame mainWindow;
	private final JTextArea txaXml;
	private final JTextArea txaXsl;
	private final JTextArea txaResult;
	private final JTable tblParams;
	private final JTable tblNamespaces;
	private final JButton btnSetParam;
	private final JButton btnRemoveParam;
	private final JButton btnRunXsl;
	private final JTextField tfXpath;
	private final JButton btnRunXpath;
	private final JButton btnAddNamespace;
	private final JButton btnRemoveNamespace;
	private final JButton btnFormatXml;

	private final Map<String, String> xslParams = new TreeMap<String, String>();
	private final List<String> xslParamsList = new ArrayList<String>();
	private final JTabbedPane tbpProcessing;

	private final Map<String, String> xpathNamespacesPrefixToUri = new TreeMap<String, String>();
	private final Map<String, String> xpathNamespacesUriToPrefix = new TreeMap<String, String>();
	private final List<String> xpathNamespacePrefixes = new ArrayList<String>();

	private final NamespaceContext namespaceContext = new NamespaceContext() {

		public Iterator<?> getPrefixes(String namespaceURI) {
			return xpathNamespacePrefixes.iterator();
		}

		public String getPrefix(String namespaceURI) {
			return xpathNamespacesUriToPrefix.get(namespaceURI);
		}

		public String getNamespaceURI(String prefix) {
			return xpathNamespacesPrefixToUri.get(prefix);
		}
	};

	public XmlWorks() {
		xslParams.put("testParam", "Param Value");
		xslParamsList.add("testParam");

		TableModel paramsTableModel = new AbstractTableModel() {
			private static final long serialVersionUID = -9169765392107524412L;

			public Object getValueAt(int rowIndex, int columnIndex) {
				Object result = null;
				if (rowIndex < xslParamsList.size()) {
					result = xslParamsList.get(rowIndex);
					if (result != null && columnIndex > 0) {
						result = xslParams.get(result);
					}
				}
				return result;
			}

			public int getRowCount() {
				return xslParamsList.size();
			}

			public int getColumnCount() {
				return 2;
			}
		};

		TableModel namespacesTableModel = new AbstractTableModel() {
			private static final long serialVersionUID = 7639530381489168188L;

			public Object getValueAt(int rowIndex, int columnIndex) {
				Object result = null;
				if (rowIndex < xpathNamespacePrefixes.size()) {
					result = xpathNamespacePrefixes.get(rowIndex);
					if (result != null && columnIndex > 0) {
						result = xpathNamespacesPrefixToUri.get(result);
					}
				}
				return result;
			}

			public int getRowCount() {
				return xpathNamespacePrefixes.size();
			}

			public int getColumnCount() {
				return 2;
			}
		};

		tbpProcessing = new JTabbedPane();

		btnSetParam = new JButton("Set parameter");
		btnRemoveParam = new JButton("Remove parameter");
		btnRunXsl = new JButton("Run XSL Transformation");
		btnRunXpath = new JButton("Evaluate XPath expression");

		btnAddNamespace = new JButton("Add namespace");
		btnRemoveNamespace = new JButton("Remove namespace");
		btnFormatXml = new JButton("Format XML");

		btnSetParam.addActionListener(this);
		btnRemoveParam.addActionListener(this);
		btnRunXsl.addActionListener(this);
		btnRunXpath.addActionListener(this);
		btnAddNamespace.addActionListener(this);
		btnRemoveNamespace.addActionListener(this);
		btnFormatXml.addActionListener(this);

		tfXpath = new JTextField("/root//test:*[contains(name(),'lem')]");
		xpathNamespacePrefixes.add("test");
		xpathNamespacesPrefixToUri.put("test", "test:namespace");
		xpathNamespacesUriToPrefix.put("test:namespace", "test");
		tblNamespaces = new JTable(namespacesTableModel);
		JPanel namespacesPanel = new JPanel(new BorderLayout());
		namespacesPanel.add(btnAddNamespace, BorderLayout.NORTH);
		namespacesPanel.add(new JScrollPane(tblNamespaces), BorderLayout.CENTER);
		namespacesPanel.add(btnRemoveNamespace, BorderLayout.SOUTH);
		JPanel xpathPanel = new JPanel(new BorderLayout());
		{
			JPanel xpathFieldPanel = new JPanel(new BorderLayout());
			xpathFieldPanel.add(new JLabel("XPath expression:"), BorderLayout.WEST);
			xpathFieldPanel.add(tfXpath, BorderLayout.CENTER);
			xpathFieldPanel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));
			xpathPanel.add(xpathFieldPanel, BorderLayout.NORTH);
		}
		xpathPanel.add(namespacesPanel, BorderLayout.CENTER);
		xpathPanel.add(btnRunXpath, BorderLayout.SOUTH);

		mainWindow = new JFrame("XmlWorks");
		mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		txaXml = new JTextArea(
				"<root>\n\t<test:elem attrOne=\"at1val\" attrTwo=\"at2val\" xmlns:test=\"test:namespace\">\n\t\tSome text\n\t\t<test:subElem>Some other text</test:subElem>\n\t</test:elem>\n\t<otherElem at3=\"three\">\n\t</otherElem>\n</root>\n");
		txaXml.setTabSize(3);
		txaXsl = new JTextArea(
				"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"  xmlns:test=\"test:namespace\">\n\t<xsl:param name=\"testParam\"/>\n\n\t<xsl:template match=\"/root/test:elem\">\n\t\t<xsl:element name=\"newElem\">\n\t\t\t<xsl:attribute name=\"newAttr\">\n\t\t\t\t<xsl:value-of select=\"@attrOne\" />\n\t\t\t\t<xsl:value-of select=\"'/'\" />\n\t\t\t\t<xsl:value-of select=\"$testParam\" />\n\t\t\t</xsl:attribute>\n\t\t</xsl:element>\n\t</xsl:template>\n</xsl:stylesheet>\n");
		txaXsl.setTabSize(3);
		txaResult = new JTextArea();
		txaResult.setTabSize(3);

		tblParams = new JTable(paramsTableModel);
		JPanel xslParamsPanel = new JPanel(new BorderLayout());
		xslParamsPanel.add(btnSetParam, BorderLayout.NORTH);
		xslParamsPanel.add(new JScrollPane(tblParams), BorderLayout.CENTER);
		xslParamsPanel.add(btnRemoveParam, BorderLayout.SOUTH);

		JSplitPane splitPaneXsl = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, new JScrollPane(txaXsl), xslParamsPanel);
		tbpProcessing.addTab("XSL Transformation", splitPaneXsl);
		tbpProcessing.addTab("XPath", xpathPanel);

		JPanel xmlPanel = new JPanel(new BorderLayout());
		xmlPanel.add(new JScrollPane(txaXml), BorderLayout.CENTER);
		xmlPanel.add(btnFormatXml, BorderLayout.SOUTH);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, xmlPanel, tbpProcessing);
		JSplitPane splitPaneTwo = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitPane, new JScrollPane(txaResult));

		Container contentPane = mainWindow.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(splitPaneTwo, BorderLayout.CENTER);
		contentPane.add(btnRunXsl, BorderLayout.SOUTH);

		splitPaneTwo.setResizeWeight(0.7);
		splitPaneXsl.setResizeWeight(0.5);
		splitPane.setResizeWeight(0.5);
		mainWindow.pack();
		mainWindow.setVisible(true);
		splitPaneXsl.setDividerLocation(0.5);
		splitPaneTwo.setDividerLocation(0.7);
		splitPane.setDividerLocation(0.5);
	}

	public static void main(String[] args) {
		new XmlWorks();
	}

	public void actionPerformed(ActionEvent actEvent) {
		if (actEvent.getSource() == btnRunXsl) {
			try {
				String sourceText = txaXml.getText();
				String xslText = txaXsl.getText();
				Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(new StringReader(xslText)));
				StringWriter stringWriter = new StringWriter();
				for (Map.Entry<String, String> xslParam : xslParams.entrySet()) {
					transformer.setParameter(xslParam.getKey(), xslParam.getValue());
				}
				transformer.transform(new StreamSource(new StringReader(sourceText)), new StreamResult(stringWriter));
				txaResult.setText(stringWriter.toString());
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, new JScrollPane(new JTextArea("Exception occurred:\n" + stacktraceToString(e))));
			}
		} else if (actEvent.getSource() == btnSetParam) {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xPath = factory.newXPath();
			xPath.setNamespaceContext(new NamespaceContext() {

				private List<String> contexts;

				{
					contexts = new ArrayList<String>(1);
					contexts.add("xsl");
				}

				public Iterator<?> getPrefixes(String namespaceURI) {
					return contexts.iterator();
				}

				public String getPrefix(String namespaceURI) {
					return namespaceURI.equals("http://www.w3.org/1999/XSL/Transform") ? "xsl" : null;
				}

				public String getNamespaceURI(String prefix) {
					return prefix.equals("xsl") ? "http://www.w3.org/1999/XSL/Transform" : null;
				}
			});

			List<String> options = new LinkedList<String>();
			try {
				XPathExpression expr = xPath.compile("/xsl:stylesheet/xsl:param/@name");
				Object result = expr.evaluate(new InputSource(new StringReader(txaXsl.getText())), XPathConstants.NODESET);

				NodeList nodes = (NodeList) result;
				for (int i = 0; i < nodes.getLength(); i++) {
					options.add(nodes.item(i).getNodeValue());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			String paramName;
			if (options.size() > 0) {
				paramName = (String) JOptionPane.showInputDialog(mainWindow, "Enter parameter name", "XSL parameter", JOptionPane.PLAIN_MESSAGE, null,
						options.toArray(), options.get(0));
			} else {
				paramName = JOptionPane.showInputDialog(mainWindow, "Enter parameter name", "XSL parameter", JOptionPane.PLAIN_MESSAGE);
			}

			if (paramName != null && paramName.trim().length() > 0) {
				String paramValue = JOptionPane.showInputDialog(mainWindow, "Enter parameter value");
				if (paramValue != null && paramValue.trim().length() > 0) {
					paramName = paramName.trim();
					paramValue = paramValue.trim();
					xslParams.put(paramName, paramValue);
					if (!xslParamsList.contains(paramName)) {
						xslParamsList.add(paramName);
					}
					tblParams.invalidate();
					tblParams.revalidate();
					tblParams.repaint();
				}
			}
		} else if (actEvent.getSource() == btnRemoveParam) {
			int selectedRow = tblParams.getSelectedRow();
			if (selectedRow >= 0) {
				String key = (String) tblParams.getModel().getValueAt(selectedRow, 0);
				xslParamsList.remove(key);
				xslParams.remove(key);
				tblParams.invalidate();
				tblParams.revalidate();
				tblParams.repaint();
			}
		} else if (actEvent.getSource() == btnRunXpath) {
			try {
				XPathFactory factory = XPathFactory.newInstance();
				XPath xPath = factory.newXPath();
				xPath.setNamespaceContext(namespaceContext);
				XPathExpression expr = xPath.compile(tfXpath.getText());
				Object result = expr.evaluate(new InputSource(new StringReader(txaXml.getText())), XPathConstants.NODESET);

				StringBuilder resultText = new StringBuilder();
				NodeList nodes = (NodeList) result;
				Transformer serializer = TransformerFactory.newInstance().newTransformer();

				for (int i = 0; i < nodes.getLength(); i++) {
					StringWriter sw = new StringWriter();
					serializer.setOutputProperty("omit-xml-declaration", "yes");
					serializer.transform(new DOMSource(nodes.item(i)), new StreamResult(sw));
					resultText.append("Node #").append(String.valueOf(i + 1)).append(": ").append(sw.toString()).append("\n");
				}
				txaResult.setText(resultText.toString());
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, new JScrollPane(new JTextArea("Exception occurred:\n" + stacktraceToString(e))));
			}
		} else if (actEvent.getSource() == btnRemoveNamespace) {
			int selectedRow = tblNamespaces.getSelectedRow();
			if (selectedRow >= 0) {
				String key = (String) tblNamespaces.getModel().getValueAt(selectedRow, 0);
				String uri = xpathNamespacesPrefixToUri.remove(key);
				xpathNamespacesUriToPrefix.remove(uri);
				xpathNamespacePrefixes.remove(key);
				tblNamespaces.invalidate();
				tblNamespaces.revalidate();
				tblNamespaces.repaint();
			}
		} else if (actEvent.getSource() == btnAddNamespace) {
			try {
				String namespacePrefix = JOptionPane.showInputDialog(mainWindow, "Enter namespace prefix", "XML namespace", JOptionPane.PLAIN_MESSAGE);

				if (namespacePrefix != null && namespacePrefix.trim().length() > 0) {
					String namespaceUri = JOptionPane.showInputDialog(mainWindow, "Enter namespace URI");
					if (namespaceUri != null && namespaceUri.trim().length() > 0) {
						namespacePrefix = namespacePrefix.trim();
						namespaceUri = namespaceUri.trim();

						if (xpathNamespacePrefixes.contains(namespacePrefix)) {
							xpathNamespacesUriToPrefix.remove(xpathNamespacesPrefixToUri.get(namespacePrefix));
							xpathNamespacesPrefixToUri.put(namespacePrefix, namespaceUri);
							xpathNamespacesUriToPrefix.put(namespaceUri, namespacePrefix);
						} else {
							xpathNamespacesPrefixToUri.put(namespacePrefix, namespaceUri);
							xpathNamespacesUriToPrefix.put(namespaceUri, namespacePrefix);
							xpathNamespacePrefixes.add(namespacePrefix);
						}

						tblNamespaces.invalidate();
						tblNamespaces.revalidate();
						tblNamespaces.repaint();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, new JScrollPane(new JTextArea("Exception occurred:\n" + stacktraceToString(e))));
			}
		} else if (actEvent.getSource() == btnFormatXml) {

			try {
				DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				final Document document = docBuilder.parse(new InputSource(new StringReader(txaXml.getText())));

				TransformerFactory factory = TransformerFactory.newInstance();
				factory.setAttribute("indent-number", new Integer(2));
				Transformer transformer = factory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

				DOMSource source = new DOMSource(document);
				StreamResult result = new StreamResult(new StringWriter());
				transformer.transform(source, result);
				String xmlString = result.getWriter().toString();
				txaXml.setText(xmlString);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, new JScrollPane(new JTextArea("Exception occurred:\n" + stacktraceToString(e))));
			}
		}
	}

	private static String stacktraceToString(Throwable t) {
		StringWriter stacktrace = new StringWriter();
		t.printStackTrace(new PrintWriter(stacktrace));
		return stacktrace.toString();
	}
}
