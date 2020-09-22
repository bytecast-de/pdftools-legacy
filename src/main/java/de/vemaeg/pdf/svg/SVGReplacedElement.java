package de.vemaeg.pdf.svg;

import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.w3c.dom.Document;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextReplacedElement;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.PageBox;
import org.xhtmlrenderer.render.RenderingContext;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.InputStream;

public class SVGReplacedElement implements ITextReplacedElement {

    private final Point location = new Point(0, 0);
    private final TranscoderInput ti;
    private int width = 0;
    private int height = 0;

    public SVGReplacedElement(Document svg) {
        this.ti = new TranscoderInput(svg);
    }

    public SVGReplacedElement(InputStream is) {
        this.ti = new TranscoderInput(is);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void detach(LayoutContext c) {
    }

    @Override
    public int getBaseline() {
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    @Override
    public boolean hasBaseline() {
        return false;
    }

    @Override
    public boolean isRequiresInteractivePaint() {
        return false;
    }

    @Override
    public Point getLocation() {
        return location;
    }

    @Override
    public void setLocation(int x, int y) {
        this.location.x = x;
        this.location.y = y;
    }

    @Override
    public void paint(RenderingContext renderingContext, ITextOutputDevice outputDevice,
                      BlockBox blockBox) {
        PdfContentByte cb = outputDevice.getWriter().getDirectContent();
        float width = this.width / outputDevice.getDotsPerPoint();
        float height = this.height / outputDevice.getDotsPerPoint();

        PdfTemplate template = cb.createTemplate(width, height);
        @SuppressWarnings("deprecation")
        Graphics2D g2d = template.createGraphics(width, height);
        PrintTranscoder prm = new PrintTranscoder();
        prm.transcode(ti, null);

        PageFormat pg = new PageFormat();
        Paper pp = new Paper();
        pp.setSize(width, height);
        pp.setImageableArea(0, 0, width, height);
        pg.setPaper(pp);
        prm.print(g2d, pg, 0);
        g2d.dispose();

        PageBox page = renderingContext.getPage();
        float x = blockBox.getAbsX() + page.getMarginBorderPadding(renderingContext, CalculatedStyle.LEFT);
        float y = (page.getBottom() - (blockBox.getAbsY() + this.height)) + page.getMarginBorderPadding(
                renderingContext, CalculatedStyle.BOTTOM);
        x /= outputDevice.getDotsPerPoint();
        y /= outputDevice.getDotsPerPoint();

        cb.addTemplate(template, x, y);
    }
}
