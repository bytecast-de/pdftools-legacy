package de.vemaeg.pdf;

import com.itextpdf.text.pdf.PdfReader;

/**
 * Zuweisungsinformationen eines PDF-Stempels = "Overlay"
 * 
 * @author	<a href="mailto:bb@dedicatedservice.de">Benjamin Bieber</a>
 * @version	$Revision$ $Date$
 * @version $Id$
 * 
 */
public class OverlayDescriptor {

	private static final int RANGE_TYPE_INVALID = 0;
	private static final int RANGE_TYPE_ALL = 1;
	private static final int RANGE_TYPE_SINGLE = 2;
	private static final int RANGE_TYPE_MULTI = 3;

	private int numPages = 0;
	private int rangeType = RANGE_TYPE_ALL;
	private int rangeFrom = 1;
	private int rangeTo = Integer.MAX_VALUE;

	private PdfReader reader;

	/**
	 * Erzeugt eine Overlay-Beschreibung aus PDF-Reader und Seitenbereichsdefinition
	 * 
	 * @param reader
	 * @param range
	 */
	public OverlayDescriptor(PdfReader reader, String range) {
		this.reader = reader;
		this.numPages = reader.getNumberOfPages();
		this.initRange(range);
	}

	/**
	 * Liefert den PDF-Reader, der den Overlay content beinhaltet
	 * 
	 * @return PdfReader
	 */
	public PdfReader getReader() {
		return this.reader;
	}

	/**
	 * Liefert die Anzahl der Seiten des Overlays
	 * 
	 * @return int
	 */
	public int getNumberOfPages() {
		return this.numPages;
	}

	/**
	 * Liest die Seitenbereichsdefinition ein
	 * 
	 * @param range Seitenbereichsdefinition
	 */
	private void initRange(String range) {
		if (range.equalsIgnoreCase("alle")) {
			// default-Werte
			return;
		}
		
		String[] tmp = range.split("-");
		if (tmp.length == 0) {
			this.rangeType = RANGE_TYPE_INVALID;	
			return;
		}
		
		this.rangeFrom = Integer.parseInt(tmp[0]);
		if (tmp.length == 2) {
			this.rangeType = RANGE_TYPE_MULTI;	
			this.rangeTo =  Integer.parseInt(tmp[1]);
		} else {				
			if (range.contains("-")) {
				this.rangeType = RANGE_TYPE_MULTI;
				this.rangeTo =  Integer.MAX_VALUE;
			} else {
				this.rangeType = RANGE_TYPE_SINGLE;
				this.rangeTo =  this.rangeFrom;
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "TYPE " + this.rangeType + " - FROM " + this.rangeFrom + " - TO " + this.rangeTo + " - SIZE " + this.numPages;
	}

	/**
	 * Liefert die minimale Seitenanzahl, die die Einbindung des Overlays erfordert
	 * 
	 * @return int 
	 */
	public int getMinimumDocumentSize() {
		if (this.rangeType == RANGE_TYPE_ALL) {
			return this.numPages;
		} 	
		
		return this.rangeFrom + (this.numPages-1);
	}

	/**
	 * Liefert die Seitenummer des Overlays, die der übergebenen Seitennummer zugewiesen ist
	 * 
	 * @param basePageNum Seitennummer
	 * @return int
	 */
	public int getAssignedPageNum(int basePageNum) {
		int numPages = this.getNumberOfPages();
		
		switch(this.rangeType) {
		case RANGE_TYPE_INVALID:
			return -1;
		case RANGE_TYPE_ALL:
			return OverlayAssignment.calculate(basePageNum, numPages, 1);
		case RANGE_TYPE_SINGLE:
			if (basePageNum < this.rangeFrom || basePageNum > this.rangeTo) {
				return -1;
			}
			return OverlayAssignment.calculate(basePageNum, numPages, this.rangeFrom);
		case RANGE_TYPE_MULTI:
			return OverlayAssignment.calculate(basePageNum, numPages, this.rangeFrom);
		}

		return -1;
	}

	/**
	 * Ist das Overlay der übergebenen Seitennummer zugewiesen?
	 * 
	 * @param basePageNum Seitennummer
	 * @return boolean
	 */
	public boolean isAssignedToPage(int basePageNum) {		
		int res = this.getAssignedPageNum(basePageNum);
		return res != -1;
	}
}