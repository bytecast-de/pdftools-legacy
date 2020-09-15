package de.vemaeg.util;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;

import de.vemaeg.common.db.dao.DAOFactory;
import de.vemaeg.common.db.dao.HibernateUtil;
import de.vemaeg.common.db.dao.MitarbeiterDAO;
import de.vemaeg.common.db.dao.MitgliedDAO;
import de.vemaeg.common.db.dao.SitzungDAO;
import de.vemaeg.common.db.dao.VersichererDAO;
import de.vemaeg.common.db.model.Mitarbeiter;
import de.vemaeg.common.db.model.Mitglied;
import de.vemaeg.common.db.model.Sitzung;
import de.vemaeg.common.db.model.Versicherer;
import de.vemaeg.common.util.UniqueIdGenerator;
import de.vemaeg.login.service.LoginDataProcessor;
import de.vemaeg.login.LoginException;
import de.vemaeg.login.LoginPrincipal;

public class LoginDataProcessorSitzung implements LoginDataProcessor {
	
	private static final Logger LOGGER = Logger.getLogger(LoginDataProcessorSitzung.class);
	
	private static final String SESSION_KEY_SITZUNG = "VEMA_Sitzung";
	
	@Override
	public void feed(HttpSession session, LoginPrincipal principal) throws LoginException {
		if (principal == null) {
			throw new LoginException("Empty authentication principal.");
		}
		
		Object sitzungObj = session.getAttribute(SESSION_KEY_SITZUNG);
		Sitzung sitzung = null;
		
		if (sitzungObj == null) {
			LOGGER.debug("Could not load Sitzung from session");
			sitzung = initSitzung(principal);
		} else {
			LOGGER.debug("Sitzung succesfully loaded from session");
			sitzung = (Sitzung) sitzungObj;
		}
		
		// Bestehender Login wurde evtl. mit anderer Auth überschrieben
		// in diesem Fall Wechsel des Sitzungs-Objekts
		if (! sitzung.getVemSecToken().equals(principal.getToken())) {
			LOGGER.debug("Token has changed from " + sitzung.getVemSecToken() + " to " + principal.getToken() + ", re-init Sitzung");
			sitzung = initSitzung(principal);
		}		
		
		session.setAttribute(SESSION_KEY_SITZUNG, sitzung);
	}
	
	public static Sitzung getSitzungFromSession(HttpSession session) {
		Object sitzungObj = session.getAttribute(SESSION_KEY_SITZUNG);
		if (sitzungObj == null) {
			return null;
		}
		
		return (Sitzung) sitzungObj;
	}
	
	private Sitzung initSitzung(LoginPrincipal principal) throws LoginException {		
		Sitzung sitzung = null;
		Session s = HibernateUtil.getSession();
		
		LOGGER.debug("Initialize Sitzung from Token: " + principal.getToken());
		
		try {
			DAOFactory daoFactory = DAOFactory.instance(DAOFactory.HIBERNATE);
			s.beginTransaction();
			
			SitzungDAO dao = daoFactory.getSitzungDAO();
			sitzung = dao.findByToken(principal.getToken());
			
			if (sitzung == null) {
				sitzung = createSitzungFromPrincipal(daoFactory, principal);
				dao.makePersistent(sitzung);
			} else {
				// init session
				Hibernate.initialize(sitzung);			
			}				

			s.getTransaction().commit();
		} catch (RuntimeException ex) {
			HibernateUtil.handleException(s, ex, true);			
			throw new LoginException("Authentication failed, error accessing/creating database session entry.");
		} finally {
			HibernateUtil.closeSession(s);
		}
		
		// Fehler vermeiden, falls VR-ID == 0
		if (sitzung.getVersicherer() != null && sitzung.getVersicherer().getId() == 0) {
			sitzung.setVersicherer(null);
		}
		
		return sitzung;
	}
	
	private Sitzung createSitzungFromPrincipal(DAOFactory daoFactory, LoginPrincipal principal) {
		Sitzung sitzung = new Sitzung();
		sitzung.setBenutzer(principal.getUsername());
		sitzung.setVemSecToken(principal.getToken());
		
		String uin = UniqueIdGenerator.getUID();
		sitzung.setUIN(uin);
		
		String loginApp = principal.getLoginApplication();
		sitzung.setBenutzergruppe(loginApp);
		
		// Sonderlocke VEMA
		if (loginApp.equals(LoginPrincipal.LOGIN_APP_MAKLER) && principal.getCompanyId() == 385) {
			sitzung.setBenutzergruppe(LoginPrincipal.LOGIN_APP_ADMIN);
		} 
		
		if (loginApp.equals(LoginPrincipal.LOGIN_APP_MAKLER)) {
			MitgliedDAO mitDao = daoFactory.getMitgliedDAO();
			Mitglied mitglied = mitDao.findById(principal.getCompanyId(), false);
			sitzung.setMitglied(mitglied);
			Hibernate.initialize(mitglied);
			
			MitarbeiterDAO arbDao = daoFactory.getMitarbeiterDAO();
			Mitarbeiter mitarbeiter = arbDao.findById(principal.getEmployeeId(), false);
			sitzung.setMitarbeiter(mitarbeiter);
			Hibernate.initialize(mitarbeiter);
		}
		
		if (loginApp.equals(LoginPrincipal.LOGIN_APP_VERSICHERER)) {
			VersichererDAO vrDao = daoFactory.getVersichererDAO();
			Versicherer vr = vrDao.findById(principal.getCompanyId(), false);
			sitzung.setVersicherer(vr);
			Hibernate.initialize(vr);
			
			// FIXME: Vr-Mitarbeiter (bisher nicht in model)
		}
		
		LOGGER.debug("Created new Sitzung with UIN: " + sitzung.getUIN());
		
		return sitzung;
	}

}
