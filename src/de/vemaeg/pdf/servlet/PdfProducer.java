package de.vemaeg.pdf.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.itextpdf.text.DocumentException;

import de.vemaeg.pdf.PdfCreator;


public class PdfProducer extends HttpServlet {

	private static final long serialVersionUID = 6547215734281291313L;

	private static class RequestData {
		// input
		public String UIN = null;
		public String url = null;
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
	    
	    //data.url = "https://bb.vemaeg.de/web/extranet.php/main/portal";
	    //data.url = "https://bb.vemaeg.de/intern/Produkt.php?PdID=30&ex3=1";
	    //data.UIN = "db7c510b553e21cc1fab28fa2d77a02d";
	    
	    //System.err.println(data.url);
	    //System.err.println(data.UIN);
	    
		if (data.url != null && data.UIN != null) {
			try {
				setRespHeaders(response, data);
				PdfCreator.createFromInternalUri(response.getOutputStream(), data.url, data.UIN);
			} catch (DocumentException e) {
				response.reset();
				throw new ServletException(e);				
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
	
	private String createFilename(RequestData data) {
		String filename = "test";
		return filename;
	}
	
	private RequestData parseRequest(HttpServletRequest request) {
		RequestData data = new RequestData();
		
		data.UIN = request.getParameter("UIN");
		data.url = request.getParameter("url");
		
		return data;
	}
}
