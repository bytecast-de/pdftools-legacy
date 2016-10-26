package de.vemaeg.pdf.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.Session;

import com.itextpdf.text.DocumentException;

import de.vemaeg.common.auth.AuthenticationDataProcessorSitzung;
import de.vemaeg.common.db.dao.DAOFactory;
import de.vemaeg.common.db.dao.HibernateUtil;
import de.vemaeg.common.db.dao.KundeDAO;
import de.vemaeg.common.db.model.Kunde;
import de.vemaeg.common.db.model.Sitzung;
import de.vemaeg.common.util.StringUtil;
import de.vemaeg.pdf.IndiPdfCreator;
import de.vemaeg.pdf.PdfCreator;
import de.vemaeg.pdf.PdfException;

public class IndiPdfServlet extends HttpServlet {
	
	private static final long serialVersionUID = -251049936448814025L;
	private static final Logger LOGGER = Logger.getLogger(IndiPdfServlet.class);	

	private static class RequestData {
		// input
		public Integer pdfId = null;	
		public List<Integer> pdfIds = null;
		public Integer vorlId = null;	
		public String kdCode = null;
		public Object daten = null;
		public String editor = null;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		doGetOrPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		doGetOrPost(request, response);
	}
	
	protected void doGetOrPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
	    RequestData data = null;
	    try {
	        data = parseRequest(request);
	    } catch (Exception e) {
	        response.getWriter().print(e.getMessage());
	        return;
	    }
	    
	    Sitzung sitzung = this.getSitzung(request);   
	    IndiPdfCreator creator = new IndiPdfCreator(sitzung);		
	    String baseUrl = request.getScheme() + "://" + request.getServerName();
	    
		try {
			setRespHeaders(response, data);
	    
			if (data.pdfId != null) {
				creator.createPDF(response.getOutputStream(), data.pdfId, data.kdCode, data.daten, data.editor, baseUrl);	
				return;
			}
		
			if (data.vorlId != null) {		
				creator.createVorlagenPDF(response.getOutputStream(), data.vorlId, data.kdCode, data.daten, baseUrl);
				return;
			}
			
			if (data.pdfIds != null) {
				List<InputStream> pdfs = new ArrayList<InputStream>();	
							
				for(Integer pdfId : data.pdfIds) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();	
					creator.createPDF(out, pdfId, data.kdCode, data.daten, data.editor, baseUrl);
					pdfs.add(new ByteArrayInputStream(out.toByteArray()));
				}

				PdfCreator.concatPDFs(pdfs, response.getOutputStream(), false);	
				return;
			}
			
			
		} catch (PdfException e) {
			response.reset();
			response.getWriter().print(e.getMessage());
			return;
		} catch (DocumentException e) {
			response.reset();
			response.getWriter().print(e.getMessage());
			return;
		}
		
		response.getWriter().print("ungueltiger Parameter");
	}
	
	private Sitzung getSitzung(HttpServletRequest request) {
		HttpSession session = request.getSession();
		if (session == null) {
			return null;
		}
		
		Object sitzungObj = session.getAttribute(AuthenticationDataProcessorSitzung.SESSION_KEY_SITZUNG);
		if (sitzungObj == null) {
			return null;
		}
		
		if (! (sitzungObj instanceof Sitzung)) {
			LOGGER.error("Invalid class in session, expected Sitzung: " + sitzungObj.getClass());
			return null;
		}
		
		return (Sitzung) sitzungObj;
	}
	
	private void setRespHeaders(HttpServletResponse response, RequestData data) {
	    response.setContentType("application/pdf");
	    response.addHeader("Pragma", "public");
	    response.addHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
	    response.addHeader("Content-Disposition", "inline; filename=" + createFilename(data) + ".pdf");
	}
	
	private RequestData parseRequest(HttpServletRequest request) throws Exception {
		RequestData data = new RequestData();	
		
		String tmp = request.getParameter("pdfId");
		if (tmp != null) {
			data.pdfId = Integer.parseInt(tmp);
		}
		
		tmp = request.getParameter("vorlId");
		if (tmp != null) {
			data.vorlId = Integer.parseInt(tmp);
		}
		
		data.kdCode = request.getParameter("kdCode");
		data.editor = request.getParameter("editor");
		
		tmp = request.getParameter("daten");
		if (tmp != null && tmp.trim().length() > 0) {		    
		    ObjectMapper mapper = new ObjectMapper();
            data.daten = mapper.readValue(tmp, Object.class);
		}
		
		String[] tmpIds = request.getParameterValues("pdfIds");
		if (tmpIds != null) {
			data.pdfIds = new ArrayList<Integer>();
			
			for(String id : tmpIds) {
				data.pdfIds.add(Integer.parseInt(id));
			}
		}
		
		return data;
	}
	
	private String createFilename(RequestData data) {
		DAOFactory daoFactory = DAOFactory.instance(DAOFactory.HIBERNATE);
		
		Session s = HibernateUtil.getSession();
		Kunde kunde = null;
		try {
			s.beginTransaction();	

			KundeDAO kDao = daoFactory.getKundeDAO();
			kunde = kDao.findByCode(data.kdCode);
			
			s.getTransaction().commit();
		} finally {
			HibernateUtil.closeSession(s);
		}	
		
		SimpleDateFormat df = new SimpleDateFormat( "-yyyy-MM-dd" );
		String filename = "Dok-"; // TODO: Bezeichnung?
		if (kunde != null) {
			filename += String.format("%s-%s",
				kunde.getNachname(),
				kunde.getVorname()				
			);
		}
		filename += df.format(new Date());
		
		filename = StringUtil.replaceUmlaute(filename);
		filename = filename.replace("\n", "").replace("\r", ""); 
		
		return filename;
	}
}
