package de.vemaeg.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;

public class SessionAwareUserAgent extends ITextUserAgent {
	
	private static final Logger LOGGER = Logger.getLogger(SessionAwareUserAgent.class);
	
	private String sessionId;
	
    public SessionAwareUserAgent(ITextOutputDevice outputDevice, String sessionId) {
		super(outputDevice);
		this.sessionId = sessionId;
	}    

	protected InputStream resolveAndOpenStream(String uri) {
        java.io.InputStream is = null;
        uri = resolveURI(uri);

        try {
            URL url = new URL(uri);
    		URLConnection conn = url.openConnection();		
    		conn.setDoInput( true );
    		conn.setDoOutput( true );		
    		conn.addRequestProperty("Cookie", "JSESSIONID=" + sessionId);
    		
    		is = conn.getInputStream();
        } catch (MalformedURLException e) {
        	LOGGER.error(e.getMessage());
        } catch (IOException e) {
        	LOGGER.error(e.getMessage());
        }

        return is;
    }

}
