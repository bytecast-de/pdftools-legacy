package de.vemaeg.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Steuert die Zuweisung von Overlays (="Stempeln") zum Basis-PDF
 * 
 * @author	<a href="mailto:bb@dedicatedservice.de">Benjamin Bieber</a>
 * @version	$Revision$ $Date$
 * @version $Id$
 */
public class OverlayAssignment {

		private List<OverlayDescriptor> descriptors = new ArrayList<OverlayDescriptor>();
		
		/**
		 * Fügt einen Descriptor zur Zuweisungstabelle hinzu
		 * 
		 * @param OverlayDescriptor d
		 */
		public void addDescriptor(OverlayDescriptor d) {
			descriptors.add(d);
		}
		
		/**
		 * Liefert alle Overlay-Descriptoren für eine Zielseite im Basis-PDF
		 * 
		 * @param int basePageNum Zielseite des Basis-PDF
		 * @return List<OverlayDescriptor> Liste von Overlays
		 */
		public List<OverlayDescriptor> getOverlays(int basePageNum) {
			List<OverlayDescriptor> ret = new ArrayList<OverlayDescriptor>();			
			for(OverlayDescriptor d : descriptors) {
				if (d.isAssignedToPage(basePageNum)) {
					ret.add(d);
				}
			}
			
			return ret;
		}
		
		/**
		 * Berechnet die Seiten-Zuweisung zwischen Overlay und Basis-PDF
		 * 
		 * @param toPage Zielseite im Basis-PDF
		 * @param numberOfPages Seiten-Anzahl im Overlay 
		 * @param startPage Startseite des Overlays im Basis-PDF
		 * @return int Seite des Overlays, die der Zielseit im Basis-PDF zugewiesen wird
		 */
		public static int calculate(int toPage, int numberOfPages, int startPage) {
			if (toPage < startPage) return -1;
			
			int assignPage = (toPage - (startPage-1)) % numberOfPages;
			if (assignPage == 0) {
				assignPage = numberOfPages;
			}
			
			return assignPage;
		}
		
}