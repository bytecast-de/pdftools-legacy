package de.vemaeg.pdf.svg;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

interface SVGElementReader {

    Document execute(Element domElement) throws SVGException;

}
