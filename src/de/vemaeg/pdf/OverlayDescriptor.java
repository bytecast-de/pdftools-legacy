package de.vemaeg.pdf;

import com.itextpdf.text.pdf.PdfReader;

/**
 * Klasse, die die Zuweisunginformationen eines PDF-Stempels kapselt
 * 
 */
public class OverlayDescriptor {

		public static final int RANGE_TYPE_ALL = 1;
		public static final int RANGE_TYPE_SINGLE = 2;
		public static final int RANGE_TYPE_MULTI = 2;
		
		private int numPages = 0;
		private int rangeType = RANGE_TYPE_ALL;
		private int rangeFrom = 1;
		private int rangeTo = 1;
		private String range;
		
		private PdfReader reader;
		
		public OverlayDescriptor(PdfReader reader, String range) {
			this.reader = reader;
			this.numPages = reader.getNumberOfPages();
			this.range = range;
			this.initRange(range);
			
			System.err.println("INIT:: " + this);
		}
		
		public PdfReader getReader() {
			return this.reader;
		}
		
		public int getNumberOfPages() {
			return this.numPages;
		}
		
		private void initRange(String range) {
			if (range.equalsIgnoreCase("alle")) {
				this.rangeType = RANGE_TYPE_ALL;	
				return;
			}
			
			this.rangeType = RANGE_TYPE_SINGLE;				
			String[] tmp = range.split("-");
			if (tmp.length == 0) {
				this.rangeFrom = 1;
				this.rangeTo = 1;
				return;
			}
			
			this.rangeFrom = Integer.parseInt(tmp[0]);
			if (tmp.length == 2) {
				this.rangeType = RANGE_TYPE_MULTI;	
				this.rangeTo =  Integer.parseInt(tmp[1]);
			}			
		}
		
		public String toString() {
			return range + ": TYPE " + this.rangeType + " - FROM " + this.rangeFrom + " - TO " + this.rangeTo + " - SIZE " + this.numPages;
		}
		
		public int checkMaxPageNumber(int baseMaxPageNum) {
			System.err.println("CHECK MAX:: "  + this);
			
			if (this.rangeType == RANGE_TYPE_ALL) {
				// alle Seiten - Maximum erweitern falls Stempel größer als Basis-PDF
				if (this.numPages > baseMaxPageNum) {
					System.err.println("RESIZE " + this.numPages);
					return this.numPages;
				}
			} else {
				// ausgewählte Seiten - Maximum erweitern, falls zugeordnete Seiten ausserhalb des Basis-PDFs liegen			
				int max = this.rangeTo + (this.numPages-1);
				if (max > baseMaxPageNum) {
					System.err.println("RESIZE " + max);
					return max;
				}
			}			

			// keine Änderung
			System.err.println("KEEP " + baseMaxPageNum);
			return baseMaxPageNum;
		}
		
		public int getAssignedPageNum(int basePageNum) {
			int numPages = this.getNumberOfPages();
			
			if (this.rangeType == RANGE_TYPE_ALL) {
				return OverlayAssignment.calculate(basePageNum, numPages, 1);
			}
			
			if (this.rangeType == RANGE_TYPE_SINGLE) {
				if (basePageNum < this.rangeFrom || basePageNum > this.rangeTo) {
					return -1;
				}
				
				return OverlayAssignment.calculate(basePageNum, numPages, this.rangeFrom);
			}
			
			if (this.rangeType == RANGE_TYPE_MULTI) {
				return OverlayAssignment.calculate(basePageNum, numPages, this.rangeFrom);
			}
			
			return -1;
		}
		
		public boolean isAssignedToPage(int basePageNum) {
			int res = this.getAssignedPageNum(basePageNum);
			System.err.println("CALC NUM " + this + " :::" + basePageNum + " -- " + res);
			return res != -1;
		}
	}