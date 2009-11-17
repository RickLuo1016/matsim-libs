/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayNetStateReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.vis.netvis.visNet;

import org.matsim.api.core.v01.network.Network;
import org.matsim.vis.netvis.NetVis;
import org.matsim.vis.netvis.streaming.StateI;
import org.matsim.vis.netvis.streaming.StreamReaderA;

/**
 * 
 * @author gunnar
 * 
 */
public class DisplayNetStateReader extends StreamReaderA {

    // -------------------- CONSTRUCTION --------------------

    public DisplayNetStateReader(Network network, String filePrefix) {
        super(network, filePrefix, NetVis.FILE_SUFFIX);
    }

    // --------------- IMPLEMENTATION OF BasicStateWriter ---------------

    @Override
		protected StateI newState() {
        return new DisplayNetState(getIndexConfig());
    }

}
