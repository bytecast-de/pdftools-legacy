package de.vemaeg.pdf.ua;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.resource.ImageResource;

public class SessionAwareUserAgent extends ITextUserAgentWithCache {
	
	private static final Logger LOGGER = Logger.getLogger(SessionAwareUserAgent.class);
	
	private String UIN;
	private String mainUri;
	private String base = "";
	
    public SessionAwareUserAgent(ITextOutputDevice outputDevice, String mainUri, String UIN) {
		super(outputDevice);
		this.UIN = UIN;
		this.mainUri = mainUri;
		this.base = getBasePath(mainUri);
		this.setBaseURL(mainUri);
	}
    
    private String getBasePath(String mainUri) {
    	StringBuilder ret = new StringBuilder();
    	
    	try {
			URL tmp = new URL(mainUri);
			ret.append(tmp.getProtocol() + "://");
			ret.append(tmp.getHost());		
		} catch (MalformedURLException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage());
		}
    	
    	return ret.toString();    	
    }
    
//    protected ImageResource createImageResource(String uri, java.awt.Image img) {
//    	System.err.println("IMAGE: " + uri);
//    	return super.createImageResource(uri, img);
//    }

	protected InputStream resolveAndOpenStream(String uri) {
        java.io.InputStream is = null;
        
//	    CookieManager cookieManager = new CookieManager();
//	    CookieHandler.setDefault(cookieManager);
        
//        CookieManager cookieManager = new CookieManager();
//	    CookieHandler.setDefault(cookieManager);
//	    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        
        try {
            URL url = new URL(uri);            
            
    		URLConnection conn = url.openConnection();		
    		conn.setDoInput( true );
    		conn.setDoOutput( true );	
    		//conn.addRequestProperty("Cookie", "VEMALogin=" + UIN);
    		
    		is = conn.getInputStream();
        } catch (MalformedURLException e) {
        	LOGGER.error(e.getMessage());
        } catch (IOException e) {
        	e.printStackTrace();
        	LOGGER.error(e.getMessage());
        }

        return is;
    }
	
	public ImageResource getImageResource(String uri) {
		// UIN an interne image-URLs hängen
		if (uri.startsWith("..") || uri.startsWith("/") || uri.startsWith(base)) {
			uri += "?UIN=" + UIN;
		}

		ImageResource res = super.getImageResource(uri);		
		return res;
	}
	
//	public CSSResource getCSSResource(String uri) {
//		System.err.println("CSS: " + uri);
//		return super.getCSSResource(uri);
//	}
	
	
	
    /**
     *  Resolve a URI.
     */
    @Override
    public String resolveURI(String uri)    {    
    	// FIXME: das ist Mist. basePath von itext sollte korrekt gesetzt sein
       if (!uri.startsWith("http") && !uri.startsWith(base)) {
    	   if (uri.startsWith("..")) {
    		   uri = mainUri + "/" + uri;
    	   } else {
    		   uri = base + uri;
    	   }
       }
       //LOGGER.debug("resolved URI --> " + uri);
       return uri;
    }

}
