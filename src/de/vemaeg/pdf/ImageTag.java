package de.vemaeg.pdf;

import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;

import de.vemaeg.common.db.dao.DAOFactory;
import de.vemaeg.common.db.dao.MitgliedDAO;
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
	 * Läd Logo-URL eines Maklers für resizing
	 * 
	 * @param mitId
	 * @param maxWidth
	 * @param maxHeight
	 * @return
	 */
	public String resizeLogo(int mitId, int maxWidth, int maxHeight) {
	    Mitglied m = loadMitglied(mitId);
	    if (m == null || m.getLogoStripped() == null) {
	        return "src=\"\"";
	    }
	    
	    String url = LOGO_BASE_URL + m.getLogoStripped();
	    String dim = getImageDimensions(url, maxWidth, maxHeight);
	    return String.format("src=\"%s\" %s ", url, dim);
	}
	
	/**
	 * Initialisiere Mitglied
	 * Wichtig: Session ist offen!
	 * 
	 * @param mitId
	 * @return
	 */
	private Mitglied loadMitglied(int mitId) {
	    if (mitId <= 0) return null;

	    Mitglied m = null;
//	    Session s = HibernateUtil.getSession();
//	    try {
//	        session.beginTransaction();   

	        DAOFactory daoFactory = DAOFactory.instance(DAOFactory.HIBERNATE);
	        MitgliedDAO mDao = daoFactory.getMitgliedDAO();
	        m = mDao.findById(mitId, false);
	        Hibernate.initialize(m);

//	        session.getTransaction().commit();
//	    } catch (org.hibernate.ObjectNotFoundException e) {
//	        m = null;
//	    } catch (RuntimeException ex) {
//	        ex.printStackTrace();
//	        m = null;
//	        HibernateUtil.handleException(session, ex, true);
////	    } finally {
////	        HibernateUtil.closeSession(s);
//	    }  

	    return m;    
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
