package de.vemaeg.pdf.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;


/**
 * Default error-handler
 * 
 * gibt bei einer von anderen Servlet geworfenen Exception einen status 500 zurück   
 *
 */
public class ErrorHandler extends HttpServlet {

	private static final long serialVersionUID = 123739704862303601L;	
	private static final Logger LOGGER = Logger.getLogger(ErrorHandler.class);
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      
		Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
		//Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
		String servletName = (String) request.getAttribute("javax.servlet.error.servlet_name");
		
		if (servletName == null){
			servletName = "Unknown";
		}
		String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
		if (requestUri == null){
			requestUri = "Unknown";
		}

		// Set response content type
		response.setContentType("text/html");

		PrintWriter out = response.getWriter();
		response.setStatus(500);
		if (throwable != null) {	
			LOGGER.error("Servlet [" + servletName + "] - URI  [" + requestUri + "]", throwable);
			
			//out.println("Servlet: " + servletName);
			//out.println("URI: " + requestUri);
			out.println("[" + throwable.getClass() + "] " + throwable.getMessage());
		}
	}
	

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}