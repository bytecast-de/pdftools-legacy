package de.vemaeg.pdf;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
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
import com.lowagie.text.Rectangle;
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
	private static final String[] FONTLIST = GlobalConfig.getInstance().getStringArray("fontlist");	
	
	public static void createFromInternalUri(OutputStream output, String uri, String UIN) throws IOException, DocumentException, ServletException {  
		// init
        ITextRenderer renderer = new ITextRenderer();
		initFonts(renderer);
		
		// eigener user agent
		SharedContext sc = renderer.getSharedContext();		
		SessionAwareUserAgent userAgent = new SessionAwareUserAgent(renderer.getOutputDevice(), uri, UIN);
		sc.setUserAgentCallback(userAgent);
		userAgent.setSharedContext(sc);
			
		String content = null;
		try {
			content = getContentFromUri(uri, UIN);    		
        } catch (MalformedURLException e) {
        	LOGGER.error(e.getMessage());
        	throw new ServletException(e);
        }
		
        if (content == null) {
        	return;
        }
        
		// entferne word-Mist
		content = cleanupWord(content);
		
		// stelle sicher, dass content x-html
		content = cleanupHTML(content);
		
		// konvertiere HTML -> PDF
		renderer.setDocumentFromString(content);
        renderer.layout();         
        renderer.createPDF(output); 
        output.flush();
        output.close();			
	}
	
	private static String getContentFromUri(String uri, String UIN) throws IOException, ServletException {
    
	    HttpClient client = new HttpClient();
	    
//	    System.err.println("START");
//	    org.apache.commons.httpclient.Cookie[] cookies = client.getState().getCookies();
//	    for(int i = 0; i < cookies.length; ++i) {
//	    	System.err.println(cookies[i]);
//	    }
	    
	    GetMethod method = new GetMethod(uri);
	    method.getParams().setCookiePolicy(CookiePolicy.DEFAULT);  	    
	    method.setRequestHeader("Cookie", "VEMALogin=" + UIN + ";");
	    
	    String responseBody = null;
	    try {
	    	// Execute the method.
	    	int statusCode = client.executeMethod(method);

	    	if (statusCode != HttpStatus.SC_OK) {
	    		throw new ServletException("GET Method failed: " + method.getStatusLine());
	    	}

	    	// Read the response body.
	    	responseBody = method.getResponseBodyAsString();	    	
	    	if (responseBody == null || responseBody.length() == 0) {
	    		LOGGER.error("empty HTTP response");
	    	    org.apache.commons.httpclient.Cookie[] cookies = client.getState().getCookies();
	    	    for(int i = 0; i < cookies.length; ++i) {
	    	    	LOGGER.error("cookie: " + cookies[i]);
	    	    }
	    	}

	    } catch (HttpException e) {
	    	LOGGER.error("Fatal protocol violation: " + e.getMessage());
	    	throw new ServletException(e);
	    } finally {
	    	// Release the connection.
	    	method.releaseConnection();
	    	
	    	 // Clear cookies etc.
	    	client.getState().clear();
	    }
	   
	    
//	    System.err.println("END");
//	    cookies = client.getState().getCookies();
//	    for(int i = 0; i < cookies.length; ++i) {
//	    	System.err.println(cookies[i]);
//	    }
	    
	    return responseBody;
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
	
	private static void initFonts(ITextRenderer renderer) {		
        ITextFontResolver resolver = renderer.getFontResolver();
        
        for(int i = 0; i < FONTLIST.length; ++i) {
        	String fontName = FONTLIST[i];
        	if (fontName == null) continue;
        	
        	String fontPath = DIRNAME_FONTS + fontName.trim();	        	
       	
        	try {
        		resolver.addFont(fontPath, true);    
        		
            	//BaseFont font = BaseFont.createFont(fontPath, BaseFont.CP1252, true);
            	//LOGGER.error(font.getAllNameEntries());
        	} catch (DocumentException e) {
        		LOGGER.error("Error loading font: " + fontPath, e);
        		continue;
        	} catch (IOException e) {
        		LOGGER.error("Error loading font: " + fontPath, e);
        		continue;
        	} 
        	
        	LOGGER.debug("Loaded font: " + fontPath);
        }
	}
	
	/*
	 * hängt pdfs zusammen
	 */
	public static void concatPDFs(List<InputStream> streamOfPDFFiles, OutputStream outputStream, boolean paginate) throws IOException, DocumentException {
		Document document = new Document();
		try {
			List<InputStream> pdfs = streamOfPDFFiles;
			List<PdfReader> readers = new ArrayList<PdfReader>();
			Iterator<InputStream> iteratorPDFs = pdfs.iterator();

			// Create Readers for the pdfs.
			while (iteratorPDFs.hasNext()) {
				InputStream pdf = iteratorPDFs.next();
				PdfReader pdfReader = new PdfReader(pdf);
				//pdfReader.removeUsageRights();
				readers.add(pdfReader);
			}
			
			// Create a writer for the outputstream
			PdfWriter writer = PdfWriter.getInstance(document, outputStream);

			document.open();
			BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			PdfContentByte cb = writer.getDirectContent(); 

			PdfImportedPage page;
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
		} finally {
			if (document.isOpen()) {
				document.close();
			}			
			if (outputStream != null) {
				outputStream.close();
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
	
	public static void pdfOverlayMulti(InputStream base, Map<InputStream, String> overlays, OutputStream outputStream) throws IOException, DocumentException, PdfException {
		
		// optionale Basis-Seite
		PdfReader baseReader = null;
		int numPages = 0;
		if (base != null) {
			baseReader = new PdfReader(base);
			numPages = baseReader.getNumberOfPages();
		}

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
		
		for(InputStream content : overlays.keySet()) {
			String seiten = overlays.get(content);
			PdfReader overlayReader = new PdfReader(content);	
			int overPages = overlayReader.getNumberOfPages();
			
			// alle Seiten
			if (seiten.compareToIgnoreCase("alle") == 0) {
				if (overPages > numPages) {
					numPages = overPages;
				}
				
				for (int i = 1; i <= numPages; ++i) {
					List<Overlay> l = pageMap.get(i);
					if (l == null) {
						pageMap.put(i, new ArrayList<Overlay>());
						l = pageMap.get(i);
					}
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

			int max = to + (overPages-1);
			if (max > numPages) {
				numPages = max;
			}
			
			for (int i = from; i <= to; ++i) {
				for (int j = 0; j < overPages; ++j) {
					List<Overlay> l = pageMap.get(i+j);
					if (l == null) {
						pageMap.put(i+j, new ArrayList<Overlay>());
						l = pageMap.get(i+j);
					}
					l.add(new Overlay(j+1, overlayReader));
				}
			}
		}
		
		if (numPages == 0) {
			throw new PdfException("Fehler: Dokument hat keine Seiten! ");	
		}
		
		Document document = new Document();		
		PdfWriter writer = PdfWriter.getInstance(document, outputStream);		
		for (int i = 1; i <= numPages; ++i) {			
			if (i > 1) {
				writer.setPageEmpty(false);  // <- leere Seiten NICHT ignorieren!				
			}	
			
			boolean pageExists = false;
			if (baseReader != null) {							
				ensurePageExists(document, i, baseReader, i);
				pageExists = true;
				PdfContentByte cbDirect = writer.getDirectContent(); 
				PdfImportedPage basePage = writer.getImportedPage(baseReader, i);	
				cbDirect.addTemplate(basePage, 0, 0);
			}
			
			// overlays
			List<Overlay> overs = pageMap.get(i);
			if (overs == null) continue;			
			for(Overlay over : overs) {				
				if (over.page > over.reader.getNumberOfPages()) {
					LOGGER.error("overlay page out of range: " + over.page);
					continue;
				}
				
				if (!pageExists) {
					ensurePageExists(document, i, over.reader, over.page);
					pageExists = true;
				}
				PdfImportedPage overPage = writer.getImportedPage(over.reader, over.page);
				PdfContentByte cbDirect = writer.getDirectContent(); 
				cbDirect.addTemplate(overPage, 0, 0);
			}			
		}

		outputStream.flush();
		document.close();
		outputStream.close();		
	}
	
	private static void ensurePageExists(Document document, int pageNum, PdfReader reader, int readerPageNum) {		
		// Seitenformat
		Rectangle rec = reader.getPageSize(readerPageNum);
		if (rec.getHeight() < rec.getWidth()) {
			//System.err.println("FORMAT: quer ");
			document.setPageSize(PageSize.A4.rotate());
		} else {
			//System.err.println("FORMAT: hoch ");
			document.setPageSize(PageSize.A4);
		}
		
		if (pageNum == 1) {
			// erzeugt gleichzeitig 1. Seite
			document.open();
		} else {	
			document.newPage();
		}
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
