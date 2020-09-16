package de.vemaeg.pdf.svg;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

public class SVGReplacedElementFactory implements ReplacedElementFactory {

    @Override
    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box,
                                                 UserAgentCallback uac, int cssWidth, int cssHeight) {

        Element element = box.getElement();

        int width = 0;
        int height = 0;

        if ( cssWidth > 0 ) {
            width = cssWidth;
        }
        if ( cssHeight > 0 ) {
            height = cssHeight;
        }

        String val = element.getAttribute("width");
        if ( val != null && val.length() > 0 ) {
            width = Integer.parseInt(val);
        }
        val = element.getAttribute("height");
        if ( val != null && val.length() > 0 ) {
            height = Integer.parseInt(val);
        }

        SVGElementReader elementReader;
        if ("svg".equals(element.getNodeName())) {
            elementReader = new SVGInlineReader();
        } else if (isSVGEmbedded(element)) {
            elementReader = new SVGObjectReader();
        } else {
            return null;
        }

        Document svgDocument;
        try {
            svgDocument = elementReader.execute(element);
        } catch (SVGException e) {
            // TODO: logging
            e.printStackTrace();
            return null;
        }

        System.err.println("READ element done: " + width + " - " + height);

        return new SVGReplacedElement(svgDocument, width, height);
    }

    private boolean isSVGEmbedded(Element element) {
        return element.getNodeName().equals("object") && element.getAttribute("type").equals("image/svg+xml");
    }

    @Override
    public void reset() {
    }

    @Override
    public void remove(Element e) {
    }

    @Override
    public void setFormSubmissionListener(FormSubmissionListener listener) {
    }
}