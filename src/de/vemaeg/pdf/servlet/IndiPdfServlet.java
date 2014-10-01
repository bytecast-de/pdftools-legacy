package de.vemaeg.pdf.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.Session;

import de.vemaeg.common.db.dao.DAOFactory;
import de.vemaeg.common.db.dao.HibernateUtil;
import de.vemaeg.common.db.dao.KundeDAO;
import de.vemaeg.common.db.model.Kunde;
import de.vemaeg.common.util.StringUtil;
import de.vemaeg.pdf.IndiPdfCreator;
import de.vemaeg.pdf.PdfException;

public class IndiPdfServlet extends HttpServlet {
	
	private static final long serialVersionUID = -251049936448814025L;
	//private static final Logger LOGGER = Logger.getLogger(IndiPdfServlet.class);	

	private static class RequestData {
		// input
		public Integer pdfId = null;	
		public Integer vorlId = null;	
		public String UIN = null;
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
		
		if (data.pdfId != null) {			
			try {
				setRespHeaders(response, data);
				IndiPdfCreator.createPDF(response.getOutputStream(), data.pdfId, data.UIN, data.kdCode, data.daten, data.editor);
			} catch (PdfException e) {
				response.reset();
				response.getWriter().print(e.getMessage());
			}
			return;
		}
		
		if (data.vorlId != null) {
			try {
				setRespHeaders(response, data);
				IndiPdfCreator.createVorlagenPDF(response.getOutputStream(), data.vorlId, data.daten);
			} catch (PdfException e) {
				response.reset();
				response.getWriter().print(e.getMessage());
			}
			return;
		}
		
		response.getWriter().print("ungueltiger Parameter");
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
		
		data.UIN = request.getParameter("UIN");
		data.kdCode = request.getParameter("kdCode");
		data.editor = request.getParameter("editor");
		
		tmp = request.getParameter("daten");
		if (tmp != null && tmp.trim().length() > 0) {		    
		    ObjectMapper mapper = new ObjectMapper();
            data.daten = mapper.readValue(tmp, Object.class);
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
