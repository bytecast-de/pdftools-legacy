package de.vemaeg.pdf.svg;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class SVGObjectReader implements SVGElementReader {

    @Override
    public Document execute(Element element) throws SVGException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new SVGException("Failed to instantiate DocumentBuilder", e);
        }

        String dataPath = element.getAttribute("data");
        if (dataPath == null) {
            throw new SVGException("Missing attribute 'data' on element <object>.");
        }

        InputStream is;
        if (dataPath.startsWith("/")) {
            is = SVGObjectReader.class.getResourceAsStream(dataPath);
        } else {
            is = getUrlInputStream(dataPath);
        }

        if (is == null) {
            throw new SVGException("Failed to get InputStream for SVG data:"  + dataPath);
        }

        Document svgDocument;
        try {
            svgDocument = documentBuilder.parse(is);
        } catch (SAXException | IOException e) {
            throw new SVGException("Failed to parse SVG data: " + dataPath);
        }

        if (svgDocument == null) {
            throw new SVGException("Empty document after parsing VSG data: " + dataPath);
        }

        return svgDocument;
    }

    private InputStream getUrlInputStream(String dataPath) throws SVGException {
        try {
            URL svgUrl = new URL(dataPath);
            return svgUrl.openStream();
        } catch (IOException e) {
            throw new SVGException("Could not open URL: " + dataPath, e);
        }
    }
}
