package de.vemaeg.pdf.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.apache.log4j.Logger;

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
		RequestData data = parseRequest(request);
		
		if (data.pdfId != null) {			
			try {
				setRespHeaders(response, data);
				IndiPdfCreator.createPDF(response.getOutputStream(), data.pdfId, data.UIN, data.kdCode);
			} catch (PdfException e) {
				response.reset();
				response.getWriter().print(e.getMessage());
			}
			return;
		}
		
		if (data.vorlId != null) {
			try {
				setRespHeaders(response, data);
				IndiPdfCreator.createVorlagenPDF(response.getOutputStream(), data.vorlId);
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
	
	private RequestData parseRequest(HttpServletRequest request) {
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
		
		return data;
	}
	
	private String createFilename(RequestData data) {
		String filename = "test.pdf";
		return filename;
	}

}
