package de.vemaeg.pdf.svg;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class SVGInlineReader implements SVGElementReader {

    @Override
    public Document execute(Element element) throws SVGException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new SVGException("Failed to instantiate DocumentBuilder", e);
        }

        Document svgDocument = documentBuilder.newDocument();
        Element svgElement = (Element) svgDocument.importNode(element, true);
        svgDocument.appendChild(svgElement);

        return svgDocument;
    }
}
