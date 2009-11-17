/* *********************************************************************** *
 * project: org.matsim.*
 * NetStateA.java
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

package org.matsim.vis.netvis.streaming;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vis.netvis.config.IndexationConfig;

/**
 * Representation of a network state. This class only collects all network
 * elements' states (i.e., all node and link states).
 * 
 * @author gunnar
 */
public abstract class NetStateA implements StateI {

    // --------------------- INSTANCE VARIABLES ---------------------

    private final IndexationConfig indexConfig;

    /*
     * Lazy state creation. Since net state io is based on subclassing, problems
     * can arise if the BufferedStateA subclasses to be created here are
     * dependent on parameters of their enclosing NetStateA subclass.
     */

    private List<BufferedStateA> states = null;

    private List<BufferedStateA> states() {
        if (states == null)
            createStates();
        return states;
    }

    private void createStates() {

        states = new ArrayList<BufferedStateA>();

        for (Node node : indexConfig.getIndexedNodeView())
            if (node != null)
                states.add(newNodeState(node));
            else
                states.add(null);

        for (Link link : indexConfig.getIndexedLinkView())
            if (link != null)
                states.add(newLinkState(link));
            else
                states.add(null);
    }

    // --------------------- CONSTRUCTION ---------------------

    protected NetStateA(IndexationConfig indexConfig) {
        this.indexConfig = indexConfig;
    }

//    // -------------------- PROTECTED GETTERS --------------------
//
//    protected IndexationConfig getIndexationConfig() {
//        return indexConfig;
//    }

    // -------------------- INTERFACE DEFINITION --------------------

    protected abstract BufferedStateA newNodeState(Node node);

    protected abstract BufferedStateA newLinkState(Link link);

    // --------------------- IMPLEMENTATION OF BasicStateI ---------------------

    public void getState() {
        try {
            for (BufferedStateA state : states())
                if (state != null)
                    state.getState();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setState() {
        try {
            for (BufferedStateA state : states())
                if (state != null)
                    state.setState();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeMyself(DataOutputStream stream) {
        try {
            for (BufferedStateA state : states())
                if (state != null)
                    state.writeToStream(stream);
                else
                    BufferedStateA.skip(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readMyself(DataInputStream stream) {
        try {
            for (BufferedStateA state : states())
                if (state != null)
                    state.readFromStream(stream);
                else
                    BufferedStateA.skip(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}