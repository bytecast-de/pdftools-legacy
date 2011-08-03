package de.vemaeg.pdf;

import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import de.vemaeg.common.db.model.Mitglied;

public class ImageTag {
	
	private static final Logger LOGGER = Logger.getLogger(ImageTag.class);
	private static final String LOGO_BASE_URL = "https://www.vemaeg.de/_img/mitglieder/";	
	private Mitglied mitglied;
	
	public ImageTag(Mitglied mitglied) {
		this.mitglied = mitglied;
	}
	
	public String resize(String url, int maxWidth, int maxHeight) {	
		if (url.compareToIgnoreCase("logo") == 0) {
			if (mitglied.getLogoStripped() != null) {
				url = LOGO_BASE_URL + mitglied.getLogoStripped();
			} else {				
				return "src=\"\"";				
			}
		}
		
		String dim = getImageDimensions(url, maxWidth, maxHeight);
		
		return String.format("src=\"%s\" %s ", url, dim);
	}	

	/**
	 * ermittelt die verkleinerten Abmessungen eines Bilds unter der gegebenen URL
	 * 
	 * @param url
	 * @param maxWidth
	 * @param maxHeight
	 * @return
	 */
	private static String getImageDimensions(String url, int maxWidth, int maxHeight) {
		int width = 0;
		int height = 0;
		try {
			BufferedImage image = ImageIO.read(new URL(url));		
			width = image.getWidth();
			height = image.getHeight();
			
			if (width > maxWidth) {
				double ratio = (double) maxWidth / (double) width;
				width = maxWidth;
				height = (int) (ratio * height);			
			}
			if (height > maxHeight) {
				double ratio = (double) maxHeight / (double) height;
				height = maxHeight;
				width = (int) (ratio * width);
			}
		} catch (Exception e) {
			LOGGER.error("Fehler beim skalieren eines Bilds: " + url, e);
			return "";
		}
	    
		String ret = String.format("width=\"%d\" height=\"%d\"", width, height);		
		return ret;
	}
}
