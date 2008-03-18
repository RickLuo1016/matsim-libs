/* *********************************************************************** *
 * project: org.matsim.*
 * SNFacilitySwitcher.java
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

package org.matsim.socialnetworks.replanning;

import org.matsim.network.NetworkLayer;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;


public class SNRandomFacilitySwitcher extends MultithreadedModuleA {
	private NetworkLayer network;
	private TravelCostI tcost;
	private TravelTimeI ttime;
	private String[] factypes;
	
    public SNRandomFacilitySwitcher(String[] factypes, NetworkLayer network, TravelCostI tcost, TravelTimeI ttime) {
    	
    }

    @Override
    public PlanAlgorithmI getPlanAlgoInstance() {
//	return new SNSecLocShortest(factypes, network, tcost, ttime);
	return new SNSecLocRandom(factypes, network, tcost, ttime);
    }



}
