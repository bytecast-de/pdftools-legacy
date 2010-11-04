package de.vemaeg.pdf;

import org.apache.log4j.Logger;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;


public class BasepathUserAgent extends ITextUserAgent {
	
	private static final Logger LOGGER = Logger.getLogger(BasepathUserAgent.class);
	
	private String basepath;

	public BasepathUserAgent(ITextOutputDevice outputDevice, String basepath) {
		super(outputDevice);
		this.basepath = basepath;
	}
	
    /**
     *  Resolve a URI.
     */
    @Override
    public String resolveURI(String uri)    {    
       if (!uri.startsWith("http") && !uri.startsWith(basepath)) {
    	   uri = basepath + uri;
       }
       LOGGER.debug("resolved URI --> " + uri);
       return uri;
    }
}
