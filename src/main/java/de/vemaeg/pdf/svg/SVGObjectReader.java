package de.vemaeg.pdf.svg;

import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class SVGObjectReader implements SVGElementReader {

    @Override
    public SVGReplacedElement execute(Element element) throws SVGException {
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
            throw new SVGException("Failed to get InputStream for SVG data:" + dataPath);
        }

        return new SVGReplacedElement(is);
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
