package de.vemaeg.pdf.svg;

import javax.xml.parsers.ParserConfigurationException;

public class SVGException extends Exception {

    public SVGException() {
        super();
    }

    public SVGException(String message) {
        super(message);
    }

    public SVGException(String message, Throwable cause) {
        super(message, cause);
    }
}
