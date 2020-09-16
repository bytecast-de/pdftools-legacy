package de.vemaeg.test.pdf;

import de.vemaeg.pdf.svg.ChainingReplacedElementFactory;
import de.vemaeg.pdf.svg.SVGReplacedElementFactory;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 *
 */
public class TestSvgSwing {

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String uri = "/svg/svg.xhtml";
                if (args.length > 0) uri = args[0];

                new TestSvgSwing().run(uri);
            }
        });
    }

    private static void usage(int i, String reason) {
        String s = "svg.ShowSVGPage" +
                "\n" +
                "Simple example to render a single XML/CSS page, " +
                "which contains embedded SVG, in a Swing JFrame/JPanel " +
                "using Flying Saucer." +
                "\n\n" +
                "Usage: \n" +
                "      java svg.ShowSVGPage [uri]" +
                "\n\n" +
                "Error: " + reason;
        System.out.println(s);
        System.exit(i);
    }

    private void run(final String uri) {
        JFrame frame = new JFrame("Show Sample XML with Embedded SVG");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // RootPanel holds the ReplacedElementFactories. Currently, this factory
        // is created on each call to layout, so we override the RootPanel method
        // and return our own--the chained factory delegates first for Swing
        // replaced element, then for SVG elements.
        ChainingReplacedElementFactory cef = new ChainingReplacedElementFactory();
        cef.addReplacedElementFactory(new SwingReplacedElementFactory());
        cef.addReplacedElementFactory(new SVGReplacedElementFactory());

        final XHTMLPanel panel = new XHTMLPanel();
        panel.getSharedContext().setReplacedElementFactory(cef);

        FSScrollPane fsp = new FSScrollPane(panel);
        frame.getContentPane().add(fsp, BorderLayout.CENTER);

        frame.setSize(1024, 768);
        frame.setVisible(true);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                System.err.println("URI is: " + uri);
                URL url = TestSvgSwing.class.getResource(uri);
                String urls = url.toExternalForm();
                System.err.println("Loading URI: " + urls);
                panel.setDocument(urls);
            }
        });
    }
}