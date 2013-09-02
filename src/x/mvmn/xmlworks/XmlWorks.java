package x.mvmn.xmlworks;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlWorks implements ActionListener {

	private final JFrame mainWindow;
	private final JTextArea txaXml;
	private final JTextArea txaXsl;
	private final JTextArea txaResult;
	private final JTable tblParams;
	private final JButton btnSetParam;
	private final JButton btnRemoveParam;
	private final JButton btnRunXsl;

	private final Map<String, String> xslParams = new TreeMap<String, String>();
	private final List<String> xslParamsList = new ArrayList<String>();

	public XmlWorks() {
		xslParams.put("testParam", "Param Value");
		xslParamsList.add("testParam");

		TableModel paramsTableModel = new AbstractTableModel() {
			private static final long serialVersionUID = -9169765392107524412L;

			@Override
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

			@Override
			public int getRowCount() {
				return xslParamsList.size();
			}

			@Override
			public int getColumnCount() {
				return 2;
			}
		};

		btnSetParam = new JButton("Set parameter");
		btnRemoveParam = new JButton("Remove parameter");
		btnRunXsl = new JButton("Run XSL Transformation");

		btnSetParam.addActionListener(this);
		btnRemoveParam.addActionListener(this);
		btnRunXsl.addActionListener(this);

		mainWindow = new JFrame("XmlWorks");
		mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		txaXml = new JTextArea(
				"<root>\n\t<elem attrOne=\"at1val\" attrTwo=\"at2val\">\n\t\tSome text\n\t</elem>\n\t<otherElem at3=\"three\">\n\t</otherElem>\n</root>\n");
		txaXml.setTabSize(3);
		txaXsl = new JTextArea("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" + "\t<xsl:param name=\"testParam\"/>\n"
				+ "\t<xsl:template match=\"/root/elem\">\n" + "\t\t<xsl:element name=\"newElem\">\n" + "\t\t\t<xsl:attribute name=\"newAttr\">\n"
				+ "\t\t\t\t<xsl:value-of select=\"@attrOne\" />\n" + "\t\t\t\t<xsl:value-of select=\"'/'\" />\n"
				+ "\t\t\t\t<xsl:value-of select=\"$testParam\" />\n" + "\t\t\t</xsl:attribute>\n" + "\t\t</xsl:element>\n" + "\t</xsl:template>\n"
				+ "</xsl:stylesheet>");
		txaXsl.setTabSize(3);
		txaResult = new JTextArea();
		txaResult.setTabSize(3);

		tblParams = new JTable(paramsTableModel);
		JPanel xslParamsPanel = new JPanel(new BorderLayout());
		xslParamsPanel.add(btnSetParam, BorderLayout.NORTH);
		xslParamsPanel.add(new JScrollPane(tblParams), BorderLayout.CENTER);
		xslParamsPanel.add(btnRemoveParam, BorderLayout.SOUTH);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, new JScrollPane(txaXml), new JScrollPane(txaXsl));
		splitPane.setDividerLocation(0.5);
		splitPane.setResizeWeight(0.5);
		JSplitPane splitPaneTwo = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitPane, new JScrollPane(txaResult));
		splitPaneTwo.setDividerLocation(0.5);
		splitPaneTwo.setResizeWeight(0.5);
		Container contentPane = mainWindow.getContentPane();
		contentPane.setLayout(new BorderLayout());
		JSplitPane splitPaneThree = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, splitPaneTwo, xslParamsPanel);
		contentPane.add(splitPaneThree, BorderLayout.CENTER);
		contentPane.add(btnRunXsl, BorderLayout.SOUTH);

		mainWindow.pack();
		splitPaneThree.setDividerLocation(0.7);
		splitPaneThree.setResizeWeight(0.7);
	}

	public static void main(String[] args) {
		new XmlWorks().mainWindow.setVisible(true);
	}

	@Override
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
				JOptionPane.showMessageDialog(mainWindow, "Exception occurred: " + e.getClass().getName() + " - " + e.getMessage());
			}
		} else if (actEvent.getSource() == btnSetParam) {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xPath = factory.newXPath();
			List<String> options = new LinkedList<String>();
			try {
				XPathExpression expr = xPath.compile("/*[name()='xsl:stylesheet']/*[name()='xsl:param']/@name");
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
		}
	}
}
