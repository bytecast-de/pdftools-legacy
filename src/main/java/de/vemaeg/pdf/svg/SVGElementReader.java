package de.vemaeg.pdf.svg;

import org.w3c.dom.Element;

interface SVGElementReader {

    SVGReplacedElement execute(Element domElement) throws SVGException;

}
