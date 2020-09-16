package de.vemaeg.test.pdf;

import com.itextpdf.text.DocumentException;
import de.vemaeg.pdf.svg.ChainingReplacedElementFactory;
import de.vemaeg.pdf.svg.SVGReplacedElementFactory;
import de.vemaeg.pdf.ua.SessionAwareUserAgent;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;

import javax.servlet.ServletException;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

/**
 *
 */
public class TestSvgPdf {

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            String uri = "/svg/simple.html";
            if (args.length > 0) uri = args[0];

            try {
                new TestSvgPdf().run(uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void run(final String fileName) throws IOException {
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
        System.out.println(content);

        // output pdf
        OutputStream output = new FileOutputStream("/var/tmp/test.pdf");
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