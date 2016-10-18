package LocationProcessorServer.controller;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import LocationProcessorServer.controller.SystemData;
import LocationProcessorServer.datastructures.*;
import LocationProcessorServer.graphStructure.*;
import LocationProcessorServer.gpxParser.*;
import LocationProcessorServer.spotMapping.*;
import LocationProcessorServer.trajectoryPreparation.GPSDataProcessor;

/**
 * Controller class to start and test the program.
 * 
 * @author simon_000
 */
public class Controller {

	/**
	 * Main method to test the program
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// record time for performance
		Date start_time = new Date();

		// initialize
		SystemData.setRoutes(new ArrayList<Route>());
		SystemData.setAbstractedBySpots(new ArrayList<Route>());
		SystemData.setAbstractedByNodes(new ArrayList<Route>());
		Grid.setMinLat(48);
		Grid.setMaxLat(52);
		Grid.setMinLong(6);
		Grid.setMaxLong(11);

		// read data sets
		ArrayList<GPS_plus> approximationData = new ArrayList<GPS_plus>();
		//Controller.getDataApprox(approximationData);
		Trajectory traApproximation = new Trajectory("UserX");
		traApproximation.setTrajectory(approximationData);

		ArrayList<GPS_plus> trainingData = new ArrayList<GPS_plus>();
		//Controller.getTrainingData(trainingData);
		Trajectory traTraining = new Trajectory("UserX");
		traTraining.setTrajectory(trainingData);

		ArrayList<GPS_plus> evaluationDataPart1 = new ArrayList<GPS_plus>();
		Controller.getEvaluationData1(evaluationDataPart1);
		Trajectory traEvaluationP1 = new Trajectory("UserY");
		traEvaluationP1.setTrajectory(evaluationDataPart1);

		ArrayList<GPS_plus> evaluationDataPart2 = new ArrayList<GPS_plus>();
		Controller.getEvaluationData2(evaluationDataPart2);
		Trajectory traEvaluationP2 = new Trajectory("UserX");
		traEvaluationP2.setTrajectory(evaluationDataPart2);
		
		ArrayList<GPS_plus> graphTest = new ArrayList<GPS_plus>();
		//Controller.getGraphTestData(graphTest);
		Trajectory traGraphtest = new Trajectory("UserX");
		traGraphtest.setTrajectory(graphTest);
		

		// clean data and search for routes in the input trajectories
		ArrayList<Route> routes = new ArrayList<Route>();
		//routes.addAll(GPSDataProcessor.splitTrajectoryByRoutes(traGraphtest));
		routes.addAll(GPSDataProcessor.splitTrajectoryByRoutes(traEvaluationP1));
		routes.addAll(GPSDataProcessor.splitTrajectoryByRoutes(traEvaluationP2));
		//routes.addAll(GPSDataProcessor.splitTrajectoryByRoutes(traApproximation));
		//routes.addAll(GPSDataProcessor.splitTrajectoryByRoutes(traTraining));

		for (int i = 0; i < routes.size(); i++) {
			Route route = routes.get(i);
			// map into spots
			route = SpotHandler.learningSpotStructure(route);
			SystemData.getRoutes().add(route);
			// abstract routes by spots
			Route abstractedBySpots = new Route(new ArrayList<GPS_plus>(), route.getUser());
			Spot lastSpot = null;
			for (int j = 0; j < route.size(); j++) {
				Spot spot = route.getTrajectory().get(j).getSpot();
				if (spot != lastSpot && spot != null) {
					abstractedBySpots.getTrajectory().add(route.getTrajectory().get(j));
					lastSpot = spot;
				}
			}
			SystemData.getAbstractedBySpots().add(abstractedBySpots);
			
			// maintain graph structue
			GraphHandler.updateGraph(route);
			// abstract by nodes
			Route abstractedByNodes = new Route(new ArrayList<GPS_plus>(), route.getUser());
			Node lastNode = null;
			for (int j = 0; j < route.size(); j++) {
				Node node = route.getTrajectory().get(j).getSpot().node;
				if (node != lastNode && node != null) {
					abstractedByNodes.getTrajectory().add(route.getTrajectory().get(j));
					lastNode = node;
				}
			}
			SystemData.getAbstractedByNodes().add(abstractedByNodes);
		}

		// measure time for performance
		Date stop_time = new Date();
		double time = stop_time.getTime() - start_time.getTime();
		time = time / 1000;
		System.out.println("Processing-Time: " + time + " seconds");

		// -------------------------------
		// print results into text files
		// -------------------------------
		
		
		FileWriter fw, fwDist, fwCross;
		try {
			fw = new FileWriter("Spots0.csv");
			BufferedWriter bw = new BufferedWriter(fw);
			fwDist = new FileWriter("SpotDistance.txt");
			BufferedWriter bwDist = new BufferedWriter(fwDist);
			fwCross = new FileWriter("Intersections0.csv");
			BufferedWriter bwCross = new BufferedWriter(fwCross);

			Spot defaultSpot = new Spot();
			defaultSpot.setSpotID(999999999);
			Spot lastSpot = defaultSpot;

			long spotsequence = 0;
			long nodesequence = 0;
			ArrayList<Spot> spots = new ArrayList<Spot>();
			ArrayList<Spot> intersects = new ArrayList<Spot>();
			for (int j = 0; j < SystemData.getRoutes().size(); j++) {
				for (int k = 0; k < SystemData.getRoutes().get(j).getTrajectory().size(); k++) {
					Spot spot = SystemData.getRoutes().get(j).getTrajectory().get(k).getSpot();
					if (spot == null) {
					} else if (lastSpot.getSpotID() != spot.getSpotID()) {
						spotsequence++;
						// Spots
						boolean contained = false;
						for (int i = 0; i < spots.size(); i++) {
							if (spots.get(i).getSpotID() == spot.getSpotID()) {
								contained = true;
								i = spots.size();
							}
						}
						if (!contained) {
							spots.add(spot);
						}
						// Distance
						if (lastSpot.getSpotID() != 999999999) {
							bwDist.write(
									"" + GPSDataProcessor.calcDistance(lastSpot.getSpotCenter(), spot.getSpotCenter()));
							bwDist.newLine();
						}
						// Intersections
						if (spot.isIntersection()) {
							nodesequence++;
							boolean containedInt = false;
							for (int i = 0; i < intersects.size(); i++) {
								if (intersects.get(i).getSpotID() == spot.getSpotID()) {
									containedInt = true;
									i = intersects.size();
								}
							}
							if (!containedInt) {
								intersects.add(spot);
							}
						}
						if (spot.getNeighbors().size() == 1) {
							nodesequence++;
						}
					}
					lastSpot = spot;
				}
				lastSpot = defaultSpot;
			}

			// Spots
			int counter = 0;
			int counterReset = 0;
			for (int i = 0; i < spots.size(); i++) {
				Spot spot = spots.get(i);
				if (counter > 2000) {
					counter = 0;
					counterReset = counterReset + 1;
					bw.close();
					fw = new FileWriter("Spots" + counterReset + ".csv");
					bw = new BufferedWriter(fw);
				}
				if (spot != null) {
					bw.write(spot.getSpotCenter().getLatitude() + ", " + spot.getSpotCenter().getLongitude() + ", "
							+ spot.getSpotID());
					bw.newLine();
					counter = counter + 1;
				}
			}

			// Intersections
			int intersectC = 0;
			int intersectR = 0;
			for (int i = 0; i < intersects.size(); i++) {
				Spot spot = intersects.get(i);
				if (intersectC > 2000) {
					intersectC = 0;
					intersectR = intersectR + 1;
					bwCross.close();
					fwCross = new FileWriter("Spots" + intersectR + ".csv");
					bwCross = new BufferedWriter(fwCross);
				}
				if (spot != null) {
					bwCross.write(spot.getSpotCenter().getLatitude() + ", " + spot.getSpotCenter().getLongitude() + ", "
							+ spot.getSpotID());
					bwCross.newLine();
					intersectC = intersectC + 1;
				}
			}
			System.out.println("Spot-sequece data points in total: " + spotsequence);
			System.out.println("Node-sequece data points in total: " + nodesequence);
			//GraphHandler.printGraphResult();
			
			bw.close();
			bwDist.close();
			bwCross.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to get data from GPX-files
	 * 
	 * @param gps_list
	 *            : target-list for GPS data
	 */
	public static void getDataApprox(ArrayList<GPS_plus> gps_list) {
		List<GPS_plus> gps_points = new ArrayList<GPS_plus>();
		for (int i = 1; i < 18; i++) {
			gps_points.addAll(GPXHandler.readGPXFile("Approximationtestdata/a1 (" + i + ").gpx"));
		}
		for (int i = 0; i < gps_points.size(); i++) {
			gps_points.get(i).setUserID("UserX");
			gps_list.add(gps_points.get(i));
		}
		System.out.println("Data read!");
	}

	/**
	 * Method to get data from GPX-files
	 * 
	 * @param gps_list
	 *            : target-list for GPS data
	 */
	public static void getTrainingData(ArrayList<GPS_plus> gps_list) {
		List<GPS_plus> gps_points = new ArrayList<GPS_plus>();
		for (int i = 1; i < 29; i++) {
			gps_points.addAll(GPXHandler.readGPXFile("Trainingdata/t" + i + ".gpx"));
		}
		
		for (int i = 0; i < gps_points.size(); i++) {
			gps_points.get(i).setUserID("UserX");
			gps_list.add(gps_points.get(i));
		}
		System.out.println("training # data points: " + gps_list.size());
	}
	

	/**
	 * Method to get data from GPX-files
	 * 
	 * @param gps_list
	 *            : target-list for GPS data
	 */
	public static void getEvaluationData1(ArrayList<GPS_plus> gps_list) {
		List<GPS_plus> gps_points = new ArrayList<GPS_plus>();
		for (int i = 1; i < 27; i++) {
			gps_points.addAll(GPXHandler.readGPXFile("Evaluationdata/r" + i + ".gpx"));
		}
		for (int i = 0; i < gps_points.size(); i++) {
			gps_points.get(i).setUserID("UserY");
			gps_list.add(gps_points.get(i));
		}
		System.out.println("set1 # data points: " + gps_list.size());
	}

	/**
	 * Method to get data from GPX-files
	 * 
	 * @param gps_list
	 *            : target-list for GPS data
	 */
	public static void getEvaluationData2(ArrayList<GPS_plus> gps_list) {
		List<GPS_plus> gps_points = new ArrayList<GPS_plus>();
		for (int i = 1; i < 52; i++) {
			gps_points.addAll(GPXHandler.readGPXFile("Evaluationdata/s" + i + ".gpx"));
		}
		for (int i = 0; i < gps_points.size(); i++) {
			gps_points.get(i).setUserID("UserX");
			gps_list.add(gps_points.get(i));
		}
		System.out.println("set2 # data points: " + gps_list.size());
	}

	
	/**
	 * Method to get data from GPX-files
	 * 
	 * @param gps_list
	 *            : target-list for GPS data
	 */
	public static void getGraphTestData(ArrayList<GPS_plus> gps_list) {
		List<GPS_plus> gps_points = new ArrayList<GPS_plus>();
		for (int i = 1; i < 8; i++) {
			gps_points.addAll(GPXHandler.readGPXFile("Graphtestdata/g1 ("+i+").gpx"));
		}
		
		for (int i = 0; i < gps_points.size(); i++) {
			gps_points.get(i).setUserID("UserX");
			gps_list.add(gps_points.get(i));
		}
		System.out.println("graphtest # data points: " + gps_list.size());
	}
}
