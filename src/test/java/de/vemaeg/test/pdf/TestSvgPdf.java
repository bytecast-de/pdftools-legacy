package de.vemaeg.test.pdf;

import com.itextpdf.text.DocumentException;
import de.vemaeg.pdf.svg.ChainingReplacedElementFactory;
import de.vemaeg.pdf.svg.SVGReplacedElementFactory;
import de.vemaeg.pdf.ua.BasepathUserAgent;
import de.vemaeg.pdf.ua.ITextUserAgentWithCache;
import org.junit.Test;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;

public class TestSvgPdf {

    @Test
    public void testSimple() throws IOException {
        create("/svg/simple.html", "/var/tmp/test-simple.pdf");
    }

    @Test
    public void testLandingpage() throws IOException {
        create("/svg/landingpage.html", "/var/tmp/test-landingpage.pdf");
    }

    @Test
    public void testExtended() throws IOException {
        create("/svg/svg.xhtml", "/var/tmp/test-extended.pdf");
    }

    @Test
    public void testBase64() throws IOException {
        create("/svg/base64.html", "/var/tmp/test-base64.pdf");
    }

    private void create(final String fileName, final String outputFilename) throws IOException {
        // init
        ITextRenderer renderer = new ITextRenderer();

        ReplacedElementFactory replacedElementFactory = renderer.getSharedContext().getReplacedElementFactory();
        ChainingReplacedElementFactory chainingReplacedElementFactory
                = new ChainingReplacedElementFactory();
        chainingReplacedElementFactory.addReplacedElementFactory(replacedElementFactory);
        chainingReplacedElementFactory.addReplacedElementFactory(new SVGReplacedElementFactory());
        renderer.getSharedContext().setReplacedElementFactory(chainingReplacedElementFactory);

        // eigener user agent
        SharedContext sc = renderer.getSharedContext();
        ITextUserAgentWithCache userAgent = new ITextUserAgentWithCache(renderer.getOutputDevice());
        sc.setUserAgentCallback(userAgent);
        userAgent.setSharedContext(sc);

        // read file
        URL url = TestSvgPdf.class.getResource(fileName);
        File file = new File(url.getFile());
        String content = new String(Files.readAllBytes(file.toPath()));

        // output pdf
        OutputStream output = new FileOutputStream(outputFilename);
        renderer.setDocumentFromString(content);
        renderer.layout();
        try {
            renderer.createPDF(output);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        output.flush();
        output.close();
    }
}