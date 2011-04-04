package de.vemaeg.pdf.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.exception.ParseErrorException;

import de.vemaeg.common.util.VelocityRenderer;

public class MacroServlet extends HttpServlet {
	
	private static final long serialVersionUID = -26009807100157400L;
	
	private static class RequestData {
		// input
		public String action = null;		
		public String content = null;
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
		
		if (data.action == null) {
			response.getWriter().print("error, no action defined");
			return;
		}
		
		if (data.action.compareToIgnoreCase("testMacro") == 0) {
			// teste makro inhalt
			if (data.content != null) {
				try {
					VelocityRenderer.getInstance("").renderTemplate(data.content, null, false);
				} catch(ParseErrorException e) {
					response.getWriter().print("Fehler im Makro: " + e.getMessage());
					return;
				}
			} else {
				response.getWriter().print("error, no content");
				return;
			} 
			
			response.getWriter().print("success");
			return;
		}
		
		response.getWriter().print("error, invalid action");
	}
	
	private RequestData parseRequest(HttpServletRequest request) {
		RequestData data = new RequestData();	
		
		data.action = request.getParameter("action");
		data.content = request.getParameter("content");
		
		return data;
	}

}
