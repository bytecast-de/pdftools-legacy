package de.vemaeg.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.hibernate.Session;

import com.itextpdf.text.DocumentException;

import de.vemaeg.common.db.dao.DAOFactory;
import de.vemaeg.common.db.dao.HibernateUtil;
import de.vemaeg.common.db.dao.KundeDAO;
import de.vemaeg.common.db.model.IndiPDF;
import de.vemaeg.common.db.model.IndiPDFVorlage;
import de.vemaeg.common.db.model.IndiPDFZrd;
import de.vemaeg.common.db.model.Kunde;
import de.vemaeg.common.db.model.Mitarbeiter;
import de.vemaeg.common.db.model.Mitglied;
import de.vemaeg.common.db.model.Sitzung;
import de.vemaeg.common.util.GlobalConfig;
import de.vemaeg.common.util.VelocityRenderer;

public class IndiPdfCreator {
	
	private static final Logger LOGGER = Logger.getLogger(IndiPdfCreator.class);
	private static final String DIRNAME_INDIPDF = GlobalConfig.getInstance().getString("dirname.indipdf");
	private static final String FILENAME_MACROS = GlobalConfig.getInstance().getString("filename.macros");
	
	private Sitzung sitzung = null;
	
	public IndiPdfCreator(Sitzung sitzung) {
		this.sitzung = sitzung;
	}
	
	public void createPDF(OutputStream outStream, Integer pdfId, String kdCode, Object daten, String editorText, String baseUrl) throws PdfException {
		if (pdfId == null || pdfId <= 0) {
			throw new PdfException("Ungueltige PDF ID " + pdfId);
		}

		Session s = HibernateUtil.getSession();
		try {			
			
			s.beginTransaction();			
			IndiPDF indiPdf = (IndiPDF) s.load(IndiPDF.class, pdfId);			
			
			Map<String, Object> context = null;
			context = createVelocityContext(s, kdCode);
			
			if (daten != null) {
			    context.put("daten", daten);
			}
			if (editorText != null) {
				context.put("editorText", editorText);	
			}
			
			// 1. "Briefpapier" erzeugen
			Map<InputStream, String> pdfs = createContentPDF(indiPdf, context, baseUrl);

			// 2. Template pdf laden (von URL)
			InputStream pdfTemplate = getTemplatePDF(indiPdf);

			// 3. zusammenführen
			PdfCreator.pdfOverlayMulti(pdfTemplate, pdfs, outStream, indiPdf.getBriefpapier());
			
			s.getTransaction().commit();

		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new PdfException("IO-Fehler, PDF ID " + pdfId + " - " + e.getMessage());
		} catch (DocumentException e) {
			LOGGER.error(e.getMessage());
			throw new PdfException("Fehler bei Dokumenten-Erzeugung, PDF ID " + pdfId);		
		} catch (RuntimeException ex) {
			//ex.printStackTrace();
			HibernateUtil.handleException(s, ex, true);
		} finally {
			HibernateUtil.closeSession(s);
		}
	}

	
	public void createVorlagenPDF(OutputStream outStream, Integer vorlId, String kdCode, Object daten, String baseUrl) throws PdfException {		
		Session s = HibernateUtil.getSession();
		try {
			s.beginTransaction();			

			IndiPDFVorlage vorlPdf = (IndiPDFVorlage) s.load(IndiPDFVorlage.class, vorlId);	
			
			Map<String, Object> context = createVelocityContext(s, kdCode);	
			if (daten != null) {
			    context.put("daten", daten);
			}

			createVorlagenPDF(outStream, vorlPdf, context, baseUrl);
			
			s.getTransaction().commit();
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new PdfException("IO-Fehler, Vorlage ID " + vorlId);
		} catch (ParseErrorException e) {
			LOGGER.error(e.getMessage());			
			throw new PdfException("Parser-Fehler, Vorlage ID " + e.getMessage());
		} catch (DocumentException e) {
			LOGGER.error(e.getMessage());
			throw new PdfException("Fehler bei Dokumenten-Erzeugung, Vorlage ID " + vorlId);
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			HibernateUtil.handleException(s, ex, true);
			throw new PdfException("Fehler bei DB-Zugriff, Vorlage ID " + vorlId);
		} finally {
			HibernateUtil.closeSession(s);
		}	
	}
	
	private static boolean createVorlagenPDF(OutputStream out, IndiPDFVorlage vorlage, Map<String, Object> context, String baseUrl) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException, DocumentException {
		String content = getContent(vorlage);				
		if (content == null) {
			LOGGER.warn("PDF Vorlage: Content is null, id " + vorlage.getId());
			return false;
		}
		
		// template durch velocity jagen
		try {
		    content = VelocityRenderer.getInstance(DIRNAME_INDIPDF).renderTemplate(content, context, false);
		} catch (Exception e) {
		    content = "FEHLER: " + e.getMessage();
		}
		
		PdfCreator.create(out, content, baseUrl);

		return true;
	}
	
	private Map<String, Object> createVelocityContext(Session s, String kdCode) {
		Map<String, Object> context = new HashMap<String, Object>();
		
		// utils
		context.put("util", IndiPdfCreator.class);		
		
		// Session-spezifische Daten
		if (sitzung == null) {
			context.put("imgTag", new ImageTag());
			
			return context;
		}
		
		// Makler
		Mitglied mit = sitzung.getMitglied();
		context.put("makler", mit);		
		context.put("imgTag", new ImageTag(mit));	
		
		// Mitarbeiter
		Mitarbeiter arb = sitzung.getMitarbeiter();
		if (arb != null) {
			context.put("mitarb", arb);
		}
		
		DAOFactory daoFactory = DAOFactory.instance(DAOFactory.HIBERNATE);		
		// Kunde
		if (kdCode != null) {
			KundeDAO kDao = daoFactory.getKundeDAO();
			Kunde kd = kDao.findByCode(kdCode);
			
			if (kd != null) {
				context.put("kunde", kd);
				
				// Ansprechpartner
				Mitarbeiter ansp = kd.getMitarbeiter();
				if (ansp != null) {
					context.put("ansprechp", ansp);
				}
			}
		}	

		
		//Versicherer vr = (Versicherer) s.load(Versicherer.class, 23);
		//context.put("vr", vr);		
		
		return context;
	}
	
	public static String heute() { // NO_UCD
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
		return format.format(new Date());
	}
	
	private static Map<InputStream, String> createContentPDF(IndiPDF indiPdf, Map<String, Object> context, String baseUrl) throws IOException, DocumentException {
		Map<InputStream, String> pdfs = new HashMap<InputStream, String>();

		// für alle zugeordneten Vorlagen...
		Iterator<IndiPDFZrd> it = indiPdf.getVorlagen().iterator();
		while (it.hasNext()) {
			IndiPDFZrd zrd = it.next();
			IndiPDFVorlage vorlage = zrd.getVorlage();

			if (zrd.getSeiten().length() == 0)  {
				LOGGER.warn("Vorlagenzuordnung ohne Seitenangabe, id " +  vorlage.getId());
				continue;
			}

			ByteArrayOutputStream o = new ByteArrayOutputStream();
			boolean success = createVorlagenPDF(o, vorlage, context, baseUrl);
			if (success) {
				pdfs.put(new ByteArrayInputStream(o.toByteArray()), zrd.getSeiten());
			}
		}
		
		return pdfs;
	}
	
	private static InputStream getTemplatePDF(IndiPDF indiPdf) throws FileNotFoundException {
		if (indiPdf.getDatei() == null || indiPdf.getDatei().length() == 0) {
			return null; 
		}
		
		String fPath = String.format("%s/%s", DIRNAME_INDIPDF, indiPdf.getDatei()); 
		File f = new File(fPath);
		
		InputStream pdfTemplate = new FileInputStream(f);		
		return pdfTemplate;
	}
	
	private static String getContent(IndiPDFVorlage vorlage) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head><style type=\"text/css\">");		
		sb.append(vorlage.getCss());	
		sb.append("</head><body>");
		sb.append("#parse(\"" + FILENAME_MACROS + "\")");
		sb.append(vorlage.getHtml());
		sb.append("</body></html>");
		return sb.toString();
	}

}
