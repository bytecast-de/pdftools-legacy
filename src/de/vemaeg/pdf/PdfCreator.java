package de.vemaeg.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextUserAgent;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

import de.vemaeg.common.util.GlobalConfig;

public class PdfCreator {	
	
	private static final Logger LOGGER = Logger.getLogger(PdfCreator.class);
	
	private static final String DIRNAME_FONTS = GlobalConfig.getInstance().getString("dirname.fonts");
	
	/*
	 *  erzeugt pdf aus vorlage + externem header -> stamper
	 */
	public static void create(String uri, String sessionId, InputStream header, OutputStream output) throws IOException, DocumentException {    
		// init
        ITextRenderer renderer = new ITextRenderer();
		initFonts(renderer);
		
		// eigener user agent
		SharedContext sc = renderer.getSharedContext();		
		SessionAwareUserAgent userAgent = new SessionAwareUserAgent(renderer.getOutputDevice(), sessionId);
		sc.setUserAgentCallback(userAgent);
		userAgent.setSharedContext(sc);
		
		LOGGER.debug("RENDER URI: " + uri);
        
		// konvertiere HTML -> PDF
        renderer.setDocument(uri);
        renderer.layout(); 
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        renderer.createPDF(os2); 
        os2.flush();
        os2.close();
        
        // lade Kopfbereich dazu
        PdfReader reader = new PdfReader(header);  
        PdfReader reader2 = new PdfReader(os2.toByteArray());                
        
        PdfStamper stamper = new PdfStamper(reader, output);         
        PdfContentByte cb = stamper.getOverContent(1);
        PdfImportedPage page = stamper.getImportedPage(reader2, 1);
        cb.addTemplate(page, 0, 0);
        stamper.close();        

        output.flush();
        output.close();
        
        header.close();
	}
	
	// erzeugt pdf aus HTML-vorlage / fertig gerendertem content (oben wird content erst noch ausgelesen) 
	public static void create(OutputStream output, String content, String basepath) throws DocumentException, IOException {
        ITextRenderer renderer = new ITextRenderer();
		initFonts(renderer);
		
		// eigener user agent
		SharedContext sc = renderer.getSharedContext();
		ITextUserAgent userAgent;
		if (basepath != null) {
			userAgent = new BasepathUserAgent(renderer.getOutputDevice(), basepath);
		} else {
			userAgent = new ITextUserAgent(renderer.getOutputDevice());
		}		

		sc.setUserAgentCallback(userAgent);
		userAgent.setSharedContext(sc);
		
		// entferne word-Mist
		content = cleanupWord(content);
		
		// stelle sicher, dass content x-html
		content = cleanupHTML(content);
		
		// konvertiere HTML -> PDF
		try {
			renderer.setDocumentFromString(content);
			renderer.layout(); 
			renderer.createPDF(output); 
		} catch (RuntimeException ex) {
			LOGGER.error("Fehler bei PDF-Erzeugung " + ex.getMessage() );
			LOGGER.error("content: " + content);
			
			renderer.setDocumentFromString("<html><head></head><body><br/><br/><br/>Fehler bei der PDF-Erzeugung. Sollte dieser Fehler weiterhin bestehen, kontaktieren Sie bitte unseren technischen Support.</body></html>");
			renderer.layout(); 
			renderer.createPDF(output); 
		}
		
        output.flush();
        output.close();
	}
	
	private static void initFonts(ITextRenderer renderer) throws DocumentException, IOException {
		String fontPath = DIRNAME_FONTS;
		
        ITextFontResolver resolver = renderer.getFontResolver();
        resolver.addFont(fontPath + "HelveticaNeueLTStd-LtCn.otf", true);
        resolver.addFont(fontPath + "HelveticaNeueLTStd-LtCnO.otf", true);
        resolver.addFont(fontPath + "HelveticaNeueLTStd-MdCn.otf", true);
        resolver.addFont(fontPath + "HelveticaNeueLTStd-MdCnO.otf", true);
        resolver.addFont(fontPath + "HelveticaNeueLTStd-UltLt.otf", true);
	}
	
	/*
	 * hängt pdfs zusammen
	 */
	public static void concatPDFs(List<InputStream> streamOfPDFFiles, OutputStream outputStream, boolean paginate) {
		Document document = new Document();
		try {
			List<InputStream> pdfs = streamOfPDFFiles;
			List<PdfReader> readers = new ArrayList<PdfReader>();
			int totalPages = 0;
			Iterator<InputStream> iteratorPDFs = pdfs.iterator();

			// Create Readers for the pdfs.
			while (iteratorPDFs.hasNext()) {
				InputStream pdf = iteratorPDFs.next();
				PdfReader pdfReader = new PdfReader(pdf);
				//pdfReader.removeUsageRights();
				readers.add(pdfReader);
				totalPages += pdfReader.getNumberOfPages();
			}
			
			// Create a writer for the outputstream
			PdfWriter writer = PdfWriter.getInstance(document, outputStream);

			document.open();
			BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			PdfContentByte cb = writer.getDirectContent(); 

			PdfImportedPage page;
			int currentPageNumber = 0;
			int pageOfCurrentReaderPDF = 0;
			Iterator<PdfReader> iteratorPDFReader = readers.iterator();

			// Loop through the PDF files and add to the output.
			while (iteratorPDFReader.hasNext()) {
				PdfReader pdfReader = iteratorPDFReader.next();

				try {
					// Create a new page in the target for each source page.
					while (pageOfCurrentReaderPDF < pdfReader.getNumberOfPages()) {
						document.newPage();
						pageOfCurrentReaderPDF++;
						page = writer.getImportedPage(pdfReader, pageOfCurrentReaderPDF);
						currentPageNumber++;						
						cb.addTemplate(page, 0, 0);
					}
				} catch (IllegalArgumentException e) {
					LOGGER.error("Fehler beim Lesen des PDF: " + e.getMessage());
				}
				pageOfCurrentReaderPDF = 0;
			}
			outputStream.flush();
			document.close();
			outputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (document.isOpen())
				document.close();
			try {
				if (outputStream != null)
					outputStream.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
	public static void pdfOverlay(InputStream pdf1, InputStream pdf2, OutputStream outputStream) throws IOException, DocumentException {
		Document document = new Document();
		PdfWriter writer = PdfWriter.getInstance(document, outputStream);
		document.open();	
		
		if (pdf1 == null || pdf2 == null) {
			LOGGER.warn("overlay: pdf is null");
		}	
		
		PdfReader templateReader = new PdfReader(pdf1);
		PdfReader contentReader = new PdfReader(pdf2);	

		for (int i = 1; i <= templateReader.getNumberOfPages(); ++i) {
			document.newPage();
			
			PdfContentByte cbDirect = writer.getDirectContent(); 
			
			PdfImportedPage templatePage = writer.getImportedPage(templateReader, i);
			cbDirect.addTemplate(templatePage, 0, 0);
			
			if (contentReader.getNumberOfPages() <= i) {
				PdfImportedPage contentPage = writer.getImportedPage(contentReader, i);
				cbDirect.addTemplate(contentPage, 0, 0);
			}			
		}

		outputStream.flush();
		document.close();
		outputStream.close();
	}
	
	public static void pdfOverlayMulti(InputStream base, Map<InputStream, String> overlays, OutputStream outputStream) throws IOException, DocumentException {
		Document document = new Document();
		PdfWriter writer = PdfWriter.getInstance(document, outputStream);
		document.open();	
		
		PdfReader baseReader = new PdfReader(base);
		int numPages = baseReader.getNumberOfPages();		

		// overlay vorbereiten...
		class Overlay {
			public Overlay(int page, PdfReader reader) {
				this.page = page;
				this.reader = reader;
			}
			public int page;
			public PdfReader reader;			
		};
		
		Map<Integer, List<Overlay>> pageMap = new HashMap<Integer, List<Overlay>>();
		for (int i = 1; i <= numPages; ++i) {
			pageMap.put(i, new ArrayList<Overlay>());
		}
		
		for(InputStream content : overlays.keySet()) {
			String seiten = overlays.get(content);
			PdfReader overlayReader = new PdfReader(content);	
			int overPages = overlayReader.getNumberOfPages();
			
			// alle Seiten
			if (seiten.compareToIgnoreCase("alle") == 0) {
				for (int i = 1; i <= numPages; ++i) {
					List<Overlay> l = pageMap.get(i);
					int overPage = i % overPages;
					if (overPage == 0) {
						overPage = overPages;
					}
					l.add(new Overlay(overPage, overlayReader));
				}
				continue;
			} 
			
			// ausgewählte Seiten
			String[] tmp = seiten.split("-");
			if (tmp.length == 0) continue;			
			int from = Integer.parseInt(tmp[0]);
			int to = from;
			if (tmp.length == 2) {
				to =  Integer.parseInt(tmp[1]);
			} 
			if (to > numPages) {
				to = numPages;
			}
			
			int j = 1;
			for (int i = from; i <= to; ++i) {
				List<Overlay> l = pageMap.get(i);
				l.add(new Overlay(j++, overlayReader));
			}
		}

		
		for (int i = 1; i <= numPages; ++i) {
			document.newPage();
			
			PdfContentByte cbDirect = writer.getDirectContent(); 
			
			PdfImportedPage basePage = writer.getImportedPage(baseReader, i);
			cbDirect.addTemplate(basePage, 0, 0);
			
			// overlays
			for(Overlay over : pageMap.get(i)) {
				if (over.page > over.reader.getNumberOfPages()) {
					LOGGER.error("overlay page out of range: " + over.page);
					continue;
				}
				
				PdfImportedPage overPage = writer.getImportedPage(over.reader, over.page);
				cbDirect.addTemplate(overPage, 0, 0);
			}			
		}

		outputStream.flush();
		document.close();
		outputStream.close();		
	}
	
	/*
	 * druckt Logo + Kopfzeile mit Seitenzahlen
	 */
	public static void stampHeader(URL logoUrl, InputStream inPDF, OutputStream outPDF, String text1, String text2, boolean mitDeckblatt) throws IOException, DocumentException {		
        PdfReader reader = new PdfReader(inPDF);    
        PdfStamper stamper = new PdfStamper(reader, outPDF);
        
        Image img1 = null;
        Image img2 = null;
        
        if (logoUrl != null) {
        	try {
	        	// Deckblatt
	        	img1 = Image.getInstance(logoUrl);  
	        	img1.scaleToFit(130, 80);        	
	            Float x = new Float(278.0);
	            Float y = 323 - img1.getScaledHeight();
	    		img1.setAbsolutePosition(x, y);
	        	
	    		// Header
	    		// Positionierung: relativ zur Ecke oben rechts
	    		img2 = Image.getInstance(logoUrl); 
	    		img2.scaleToFit(90, 60);    		
	            x = PageSize.A4.getRight() - 15 - img2.getScaledWidth();
	            y = PageSize.A4.getTop() - 15 - img2.getScaledHeight();
	    		img2.setAbsolutePosition(x, y);    
        	} catch (IllegalArgumentException e) {
        		LOGGER.warn("PDF Logo Konvertierung fehlgeschlagen: " + logoUrl + " - " + e.getMessage());
        	} catch (FileNotFoundException e) {
        		LOGGER.warn("PDF Logo nicht gefunden: " + logoUrl );
        	}
        } 
        
        // Font
        String fontPath = DIRNAME_FONTS;
        BaseFont bf = BaseFont.createFont(fontPath + "HelveticaNeueLTStd-LtCnO.otf", "Cp1252", BaseFont.EMBEDDED);
        //System.err.println("FONT PATH: " + fontPath + "HelveticaNeueLTStd-LtCnO.otf");
        // System.err.println("BF: " + bf);
        
        PdfContentByte cb;
        int total = reader.getNumberOfPages();
        
        for (int i = 1; i <= total; i++) {
        	cb = stamper.getUnderContent(i);
        	
        	// Deckblatt? -> erste Seite keine Kopfzeile
        	if (i == 1 && mitDeckblatt) {
        		if (img1 != null) {
	        		cb.addImage(img1);
        		}
        		continue;
        	}        	
        	
        	if (img2 != null) {
        		cb.addImage(img2);
        	}
        	
        	// Headline
        	float text1Size = bf.getWidthPoint(text1, 16);        	
        	final float DISTANCE = 5; 
        	
        	String pages = String.format("seite %02d/%02d", i, total);
        	
        	float topPos = PageSize.A4.getTop() - 70;
        	float leftPos = PageSize.A4.getLeft() + 80;
        	
        	// Text 1
        	cb.beginText();
        	cb.setFontAndSize(bf, 16);        	
            cb.setTextMatrix(leftPos, topPos);
            cb.showText(text1);
            cb.endText();
            
            // Linie 1
            leftPos = leftPos + text1Size + DISTANCE;            
            drawLine(cb, leftPos, topPos);
            leftPos = leftPos + DISTANCE;
            
            if (text2 != null && text2.trim().length() > 0) {
            	float text2Size = bf.getWidthPoint(text2, 16);
            	
	            // Text 2
	            cb.beginText();            
	            cb.setTextMatrix(leftPos, topPos);
	            cb.showText(text2);
	            cb.endText();            
	            
	            // Linie 2
	            leftPos = leftPos + text2Size + DISTANCE;   
	            drawLine(cb, leftPos, topPos);
	            leftPos = leftPos + DISTANCE;
            }
            
            // Seitenzahl
            cb.beginText();  
            cb.setFontAndSize(bf, 12);
            cb.setColorFill(new Color(0x53, 0x53, 0x55));            
            cb.setTextMatrix(leftPos, topPos);            
            cb.showText(pages);              
            cb.endText();
        }
        
        stamper.close();  
	}
	
	private static void drawLine(PdfContentByte cb, float x, float y) {
        cb.setLineWidth((float) 0.6);
        cb.moveTo(x, y);
        cb.lineTo(x + (float) 4.0, y + (float) 19.0);
        cb.stroke();
	}
	
	private static String cleanupHTML(String input) {		
		// FIXME: instanz nur einmal!
		Tidy tidy = new Tidy();		
		//tidy.setXmlOut(true);
		
		// xml-input?
		tidy.setXmlTags(false);
		
		// word-mist raus
		tidy.setWord2000(true);
		
		// output-config
		tidy.setWraplen(0);		
		tidy.setRawOut(true);
		tidy.setXHTML(true);
		tidy.setOutputEncoding("UTF-8");
		
		// error-config
		tidy.setShowErrors(0);
		tidy.setShowWarnings(false);		
		tidy.setQuiet(true);		
		
		StringWriter out = new StringWriter(); 
		tidy.parse(new StringReader(input), out);
		
		return out.toString();
	}
	
	private static String cleanupWord(String str) {
		List<String> sc = new ArrayList<String>();
		
		// Kommentare 
		sc.add("(?s)<!--(.*?)-->");
		
		// class & style 
		//sc.add("(?si)class=\"(.*?)\"");
		//sc.add("(?si)class=[a-z0-9]+");
		//sc.add("(?si)style=\"(.*?)\"");
		
		// unnötiges tags: font
		sc.add("(?si)</?(font)(.*?)>");		
		
		// xml namespaces 
		sc.add("(?si)<\\?xml:namespace(.*?)>");		
		sc.add("(?si)(?<=</?)[a-z0-9]:");
		
		for (String s : sc) {
			str = str.replaceAll(s, "");
		}
		
		return str;
	}
}
