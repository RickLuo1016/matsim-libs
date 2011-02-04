/* *********************************************************************** *
 * project: org.matsim.*
 * Receiver.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package org.matsim.vis.otfvis.data;

/**
 * This sounds like "quad tree", but I think it is in the sense of "polygon with 4 corners".  kai, feb'11  
 */
public interface OTFDataQuadReceiver extends OTFDataReceiver {
	
	public void setQuad(float startX, float startY, float endX, float endY);
	
	public void setQuad(float startX, float startY, float endX, float endY, int nrLanes);
	
	public void setColor(float coloridx);
	
	public void setId(char[] idBuffer);
	
}
