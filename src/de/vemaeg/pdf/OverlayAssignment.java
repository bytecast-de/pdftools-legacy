package de.vemaeg.pdf;

import java.util.ArrayList;
import java.util.List;

public class OverlayAssignment {

		private List<OverlayDescriptor> descriptors = new ArrayList<OverlayDescriptor>();
		
		public void addDescriptor(OverlayDescriptor d) {
			descriptors.add(d);
		}
		
		public List<OverlayDescriptor> getOverlays(int basePageNum) {
			List<OverlayDescriptor> ret = new ArrayList<OverlayDescriptor>();			
			for(OverlayDescriptor d : descriptors) {
				if (d.isAssignedToPage(basePageNum)) {
					ret.add(d);
				}
			}
			
			return ret;
		}
		
		public static int calculate(int toPage, int numberOfPages, int startPage) {
			if (toPage < startPage) return -1;
			
			int assignPage = (toPage - (startPage-1)) % numberOfPages;
			if (assignPage == 0) {
				assignPage = numberOfPages;
			}
			
			return assignPage;
		}
		
}