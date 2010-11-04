package de.vemaeg.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.hibernate.Session;

import com.lowagie.text.DocumentException;

import de.vemaeg.common.db.dao.DAOFactory;
import de.vemaeg.common.db.dao.HibernateUtil;
import de.vemaeg.common.db.dao.KundeDAO;
import de.vemaeg.common.db.model.IndiPDF;
import de.vemaeg.common.db.model.IndiPDFVorlage;
import de.vemaeg.common.db.model.IndiPDFZrd;
import de.vemaeg.common.db.model.Kunde;
import de.vemaeg.common.db.model.Mitarbeiter;
import de.vemaeg.common.db.model.Mitglied;
import de.vemaeg.common.db.model.Versicherer;
import de.vemaeg.common.util.GlobalConfig;
import de.vemaeg.util.VelocityRenderer;

public class IndiPdfCreator {
	
	private static final Logger LOGGER = Logger.getLogger(IndiPdfCreator.class);
	private static final String DIRNAME_INDIPDF = GlobalConfig.getInstance().getString("dirname.indipdf");
	
	public static void createPDF(OutputStream outStream, Integer pdfId) throws PdfException {
		if (pdfId == null || pdfId <= 0) {
			throw new PdfException("Ungueltige PDF ID " + pdfId);
		}
		
		Session s = HibernateUtil.getSession();
		try {			
			
			s.beginTransaction();			
			IndiPDF indiPdf = (IndiPDF) s.load(IndiPDF.class, pdfId);	
			
			// TODO: DB nach übergebenen IDs auslesen - velocity context vorbereiten
			Map<String, Object> context = createVelocityContext(s);
			
			// 1. "Briefpapier" erzeugen
			Map<String, InputStream> pdfs = createContentPDF(indiPdf, context);

			// 2. Template pdf laden (von URL)
			InputStream pdfTemplate = getTemplatePDF(indiPdf);

			// 3. zusammenführen
			PdfCreator.pdfOverlayMulti(pdfTemplate, pdfs, outStream);
			
			s.getTransaction().commit();

		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new PdfException("IO-Fehler, PDF ID " + pdfId + " - " + e.getMessage());
		} catch (DocumentException e) {
			LOGGER.error(e.getMessage());
			throw new PdfException("Fehler bei Dokumenten-Erzeugung, PDF ID " + pdfId);		
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			HibernateUtil.handleException(s, ex, true);
		} finally {
			HibernateUtil.closeSession(s);
		}
	}

	
	public static void createVorlagenPDF(OutputStream outStream, Integer vorlId) throws PdfException {
		
		Session s = HibernateUtil.getSession();
		try {
			s.beginTransaction();			

			IndiPDFVorlage vorlPdf = (IndiPDFVorlage) s.load(IndiPDFVorlage.class, vorlId);	
			createVorlagenPDF(outStream, vorlPdf, null);
			
			s.getTransaction().commit();
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			HibernateUtil.handleException(s, ex, true);
			throw new PdfException("Fehler bei DB-Zugriff, Vorlage ID " + vorlId);
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new PdfException("IO-Fehler, Vorlage ID " + vorlId);
		} catch (DocumentException e) {
			LOGGER.error(e.getMessage());
			throw new PdfException("Fehler bei Dokumenten-Erzeugung, Vorlage ID " + vorlId);
		} finally {
			HibernateUtil.closeSession(s);
		}	
	}
	
	private static boolean createVorlagenPDF(OutputStream out, IndiPDFVorlage vorlage, Map<String, Object> context) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException, DocumentException {
		String content = getContent(vorlage);				
		if (content == null) {
			LOGGER.warn("PDF Vorlage: Content is null, id " + vorlage.getId());
			return false;
		}
		
		// template durch velocity jagen
		if (context != null) {
			content = VelocityRenderer.getInstance().renderTemplate(content, context, false);	
		}
		
		PdfCreator.create(out, content, null);
		return true;
	}
	
	private static Map<String, Object> createVelocityContext(Session s) {
		Mitglied mit = (Mitglied) s.load(Mitglied.class, 385);
		Mitarbeiter arb = (Mitarbeiter) s.load(Mitarbeiter.class, 6811);
		Versicherer vr = (Versicherer) s.load(Versicherer.class, 23);		
		
		DAOFactory daoFactory = DAOFactory.instance(DAOFactory.HIBERNATE);
		KundeDAO dao = daoFactory.getKundeDAO();
		Kunde kd = dao.findByCode("7623f75ed2d18885db28ac22");

		Map<String, Object> context = new HashMap<String, Object>();
		context.put("makler", mit);
		context.put("mitarb", arb);
		context.put("vr", vr);
		context.put("kunde", kd);
		
		return context;
	}
	
	private static Map<String, InputStream> createContentPDF(IndiPDF indiPdf, Map<String, Object> context ) throws IOException, DocumentException {
		Map<String, InputStream> pdfs = new HashMap<String, InputStream>();

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
			boolean success = createVorlagenPDF(o, vorlage, context);
			if (success) {
				pdfs.put(zrd.getSeiten(), new ByteArrayInputStream(o.toByteArray()));
			}
		}	
		
		return pdfs;
	}
	
	private static InputStream getTemplatePDF(IndiPDF indiPdf) throws FileNotFoundException {
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
		sb.append(vorlage.getHtml());
		sb.append("</body></html>");
		return sb.toString();

	}

}
