/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

import java.io.*;
import java.util.*;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.*;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.dvrp.util.gis.Schedules2GIS;
import org.matsim.contrib.dvrp.util.time.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import pl.poznan.put.util.*;
import playground.michalm.demand.taxi.PersonCreatorWithRandomTaxiMode;
import playground.michalm.taxi.*;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.filter.*;
import playground.michalm.taxi.scheduler.*;
import playground.michalm.taxi.util.chart.TaxiScheduleChartUtils;
import playground.michalm.taxi.util.stats.*;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;
import playground.michalm.util.MovingAgentsRegister;


class TaxiLauncher
{
    final String dir;
    final String netFile;
    final String plansFile;

    final String taxiCustomersFile;
    String taxisFile;
    final String ranksFile;

    final String eventsFile;
    final String changeEventsFile;

    AlgorithmConfig algorithmConfig;

    Integer nearestRequestsLimit;//null ==> no filtration
    Integer nearestVehiclesLimit;//null ==> no filtration

    Boolean onlineVehicleTracker;
    Boolean advanceRequestSubmission;
    //Double pickupTripTimeLimit;

    Boolean destinationKnown;
    Double pickupDuration;
    Double dropoffDuration;

    final boolean otfVis;

    final String vrpOutDir;
    final String histogramOutDir;
    final String eventsOutFile;

    MatsimVrpContext context;
    final Scenario scenario;

    private TravelTimeCalculator travelTimeCalculator;
    private LeastCostPathCalculatorWithCache routerWithCache;
    private VrpPathCalculator pathCalculator;

    LegHistogram legHistogram;
    TaxiDelaySpeedupStats delaySpeedupStats;
    LeastCostPathCalculatorCacheStats cacheStats;


    static Map<String, String> getDefaultParams()
    {
        Map<String, String> params = new HashMap<>();

        params.put("dir", "D:\\PP-rad\\taxi\\mielec-2-peaks\\");
        params.put("netFile", "network.xml");
        params.put("plansFile", "output\\ITERS\\it.20\\20.plans.xml.gz");

        params.put("taxiCustomersFile", "taxiCustomers_05_pc.txt");
        //params.put("ranksFile", null);
        params.put("taxisFile", "ranks-5_taxis-50.xml");

        params.put("eventsFile", "output\\ITERS\\it.20\\20.events.xml.gz");
        //params.put("changeEventsFile", null);

        //optimizer:
        params.put("algorithmConfig", "NOS_DSE_SL");

        params.put("nearestRequestsLimit", "20");
        params.put("nearestVehiclesLimit", "20");
        //params.put("pickupTripTimeLimit", "600");

        params.put("onlineVehicleTracker", "");
        //params.put("advanceRequestSubmission", "");

        //scheduler:
        //params.put("destinationKnown", "");
        params.put("pickupDuration", "120");
        params.put("dropoffDuration", "60");

        //params.put("otfVis", "");

        //params.put("vrpOutDir", "vrp_output");
        //params.put("histogramOutDir", "histograms");
        //params.put("eventsOutFile", "events.out.xml.gz");

        return params;
    }


    static Map<String, String> readParams(String paramFile)
    {
        Map<String, String> params = ParameterFileReader.readParametersToMap(new File(paramFile));
        params.put("dir", new File(paramFile).getParent() + '/');
        return params;
    }


    TaxiLauncher(Map<String, String> params)
    {
        dir = params.get("dir");
        netFile = getFilePath(params, "netFile");
        plansFile = getFilePath(params, "plansFile");

        taxiCustomersFile = getFilePath(params, "taxiCustomersFile");
        ranksFile = getFilePath(params, "ranksFile");
        taxisFile = getFilePath(params, "taxisFile");

        eventsFile = getFilePath(params, "eventsFile");
        changeEventsFile = getFilePath(params, "changeEventsFile");

        algorithmConfig = AlgorithmConfig.valueOf(params.get("algorithmConfig"));

        nearestRequestsLimit = Integer.valueOf(params.get("nearestRequestsLimit"));
        nearestVehiclesLimit = Integer.valueOf(params.get("nearestVehiclesLimit"));

        onlineVehicleTracker = params.containsKey("onlineVehicleTracker");
        advanceRequestSubmission = params.containsKey("advanceRequestSubmission");

        destinationKnown = params.containsKey("destinationKnown");
        pickupDuration = Double.valueOf(params.get("pickupDuration"));
        dropoffDuration = Double.valueOf(params.get("dropoffDuration"));

        otfVis = params.containsKey("otfVis");

        vrpOutDir = getFilePath(params, "vrpOutDir");
        histogramOutDir = getFilePath(params, "histogramOutDir");
        eventsOutFile = getFilePath(params, "eventsOutFile");

        if (changeEventsFile != null && onlineVehicleTracker) {
            System.err.println("Online vehicle tracking may not be useful -- "
                    + "travel times should be (almost?) deterministic for a time variant network");
        }

        scenario = VrpLauncherUtils.initScenario(netFile, plansFile, changeEventsFile);

        if (taxiCustomersFile != null) {
            List<String> passengerIds = PersonCreatorWithRandomTaxiMode
                    .readTaxiCustomerIds(taxiCustomersFile);
            VrpPopulationUtils.convertLegModes(passengerIds, TaxiRequestCreator.MODE, scenario);
        }

        //TaxiDemandUtils.preprocessPlansBasedOnCoordsOnly(scenario);
    }


    private String getFilePath(Map<String, String> params, String key)
    {
        String fileName = params.get(key);
        return fileName == null ? null : dir + fileName;
    }


    void initVrpPathCalculator()
    {
        TravelTime travelTime = travelTimeCalculator == null ? //
                VrpLauncherUtils.initTravelTime(scenario, algorithmConfig.ttimeSource, eventsFile) : //
                travelTimeCalculator.getLinkTravelTimes();

        TravelDisutility travelDisutility = VrpLauncherUtils.initTravelDisutility(
                algorithmConfig.tdisSource, travelTime);

        LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), travelDisutility,
                travelTime);

        TimeDiscretizer timeDiscretizer = (algorithmConfig.ttimeSource == TravelTimeSource.FREE_FLOW_SPEED && //
        !scenario.getConfig().network().isTimeVariantNetwork()) ? //
                TimeDiscretizer.CYCLIC_24_HOURS : //
                TimeDiscretizer.CYCLIC_15_MIN;

        routerWithCache = new LeastCostPathCalculatorWithCache(router, timeDiscretizer);
        pathCalculator = new VrpPathCalculatorImpl(routerWithCache, travelTime, travelDisutility);
    }


    void clearVrpPathCalculator()
    {
        travelTimeCalculator = null;
        routerWithCache = null;
        pathCalculator = null;
    }


    /**
     * Can be called several times (1 call == 1 simulation)
     */
    void go(boolean warmup)
    {
        MatsimVrpContextImpl contextImpl = new MatsimVrpContextImpl();
        this.context = contextImpl;

        contextImpl.setScenario(scenario);

        TaxiData taxiData = TaxiLauncherUtils.initTaxiData(scenario, taxisFile, ranksFile);
        contextImpl.setVrpData(taxiData);

        TaxiOptimizerConfiguration optimizerConfig = createOptimizerConfiguration();
        TaxiOptimizer optimizer = algorithmConfig.createTaxiOptimizer(optimizerConfig);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
        contextImpl.setMobsimTimer(qSim.getSimTimer());

        qSim.addQueueSimulationListeners(optimizer);

        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(
                TaxiRequestCreator.MODE, new TaxiRequestCreator(), optimizer, context, qSim);

        if (advanceRequestSubmission) {
            // yy to my ears, this is not completely clear.  I don't think that it enables advance request submission
            // for arbitrary times, but rather requests all trips before the simulation starts.  Doesn't it?  kai, jul'14

            //Yes. For a fully-featured advanced request submission process, use TripPrebookingManager, michalm, sept'14
            qSim.addQueueSimulationListeners(new BeforeSimulationTripPrebooker(passengerEngine));
        }

        LegCreator legCreator = onlineVehicleTracker ? VrpLegs.createLegWithOnlineTrackerCreator(
                optimizer, qSim.getSimTimer()) : VrpLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR;

        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                pickupDuration);

        VrpLauncherUtils.initAgentSources(qSim, context, optimizer, actionCreator);

        EventsManager events = qSim.getEventsManager();

        EventWriter eventWriter = null;
        if (eventsOutFile != null) {
            eventWriter = new EventWriterXML(eventsOutFile);
            events.addHandler(eventWriter);
        }

        if (warmup) {
            if (travelTimeCalculator == null) {
                travelTimeCalculator = TravelTimeCalculators.createTravelTimeCalculator(scenario);
            }

            events.addHandler(travelTimeCalculator);
        }
        else {
            optimizerConfig.scheduler.setDelaySpeedupStats(delaySpeedupStats);
        }

        MovingAgentsRegister mar = new MovingAgentsRegister();
        events.addHandler(mar);

        if (otfVis) { // OFTVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, false, ColoringScheme.taxicab);
        }

        if (histogramOutDir != null) {
            events.addHandler(legHistogram = new LegHistogram(300));
        }

        qSim.run();

        events.finishProcessing();

        if (eventsOutFile != null) {
            eventWriter.closeFile();
        }

        // check if all reqs have been served
        for (TaxiRequest r : taxiData.getTaxiRequests()) {
            if (r.getStatus() != TaxiRequestStatus.PERFORMED) {
                throw new IllegalStateException();
            }
        }

        if (cacheStats != null) {
            cacheStats.updateStats(routerWithCache);
        }
    }


    private TaxiOptimizerConfiguration createOptimizerConfiguration()
    {
        TaxiSchedulerParams params = new TaxiSchedulerParams(destinationKnown, pickupDuration,
                dropoffDuration);
        TaxiScheduler scheduler = new TaxiScheduler(context, pathCalculator, params);
        VehicleRequestPathFinder vrpFinder = new VehicleRequestPathFinder(pathCalculator, scheduler);
        FilterFactory filterFactory = new DefaultFilterFactory(scheduler, nearestRequestsLimit,
                nearestVehiclesLimit);

        return new TaxiOptimizerConfiguration(context, pathCalculator, scheduler, vrpFinder,
                filterFactory, algorithmConfig.goal, dir);
    }


    void generateOutput()
    {
        PrintWriter pw = new PrintWriter(System.out);
        pw.println(algorithmConfig.name());
        pw.println("m\t" + context.getVrpData().getVehicles().size());
        pw.println("n\t" + context.getVrpData().getRequests().size());
        pw.println(TaxiStats.HEADER);
        TaxiStats stats = new TaxiStatsCalculator().calculateStats(context.getVrpData()
                .getVehicles());
        pw.println(stats);
        pw.flush();

        if (vrpOutDir != null) {
            new Schedules2GIS(context.getVrpData().getVehicles(),
                    TransformationFactory.WGS84_UTM33N).write(vrpOutDir);
        }

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(TaxiScheduleChartUtils.chartSchedule(context.getVrpData()
                .getVehicles()));

        if (histogramOutDir != null) {
            VrpLauncherUtils.writeHistograms(legHistogram, histogramOutDir);
        }
    }


    public static void main(String... args)
    {
        Map<String, String> params;
        if (args.length == 0) {
            params = getDefaultParams();
        }
        else if (args.length == 1) {
            params = readParams(args[0]);
        }
        else {
            throw new IllegalArgumentException();
        }

        TaxiLauncher launcher = new TaxiLauncher(params);
        launcher.initVrpPathCalculator();
        launcher.go(false);
        launcher.generateOutput();
    }
}
