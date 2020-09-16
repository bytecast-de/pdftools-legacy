package de.vemaeg.test.pdf;

import com.itextpdf.text.DocumentException;
import de.vemaeg.pdf.svg.ChainingReplacedElementFactory;
import de.vemaeg.pdf.svg.SVGReplacedElementFactory;
import org.junit.Test;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
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
    public void testExtended() throws IOException {
        create("/svg/svg.xhtml", "/var/tmp/test-extended.pdf");
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