/**
 * 
 */
package org.science.routeanalytics;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

/**
 * @author besn
 *
 */
public class GenerateNonExperiencePoints {

	class NEPPoint {

		private int respondentid;
		private int sourceRouteID;
		private LineString segment;
		private Point geometry;
		private LineString originDestination;
		private int routeID;

		public LineString getOriginDestination() {
			return originDestination;
		}


		public void setOriginDestination(LineString originDestination) {
			this.originDestination = originDestination;
		}


		public NEPPoint(Point p,  int respondentid) {
			this.geometry = p;
			this.respondentid = respondentid;
			this.sourceRouteID = sourceRouteID;
		}


		public int getSourceRouteID() {
			return sourceRouteID;
		}

		public void setSourceRouteID(int sourceRouteID) {
			this.sourceRouteID = sourceRouteID;
		}


		public LineString getSegment() {

			return segment;
		}


		public Point getGeometry() {
			return geometry;
		}

		public void setGeometry(Point geometry) {
			this.geometry = geometry;
		}

		public int getRespondentid() {
			return respondentid;
		}

		public void setRespondentid(int respondentid) {
			this.respondentid = respondentid;
		}

		public void setSegment(LineString segment) {
			this.segment = segment;

		}




		public void setOD(LineString originDestination) {
			this.originDestination = originDestination;
			
		}


		public Double getODAngle() {
			// System.out.println(nearSegment);
			return getAngle(originDestination,
					this.getSegment()
					);
			
		}


		public Geometry getOD() {
			return this.originDestination;
		}


		public int getRouteID() {
			// TODO Auto-generated method stub
			return routeID;
		}
		
		public void setRouteID(int rid) {
			this.routeID = rid;
		}
		

	}

	private String polyLineFilename;
	private int measureDistance;
	private String polylineTypeName;
	private SimpleFeatureSource polylineSource;
	private File polyinefile;
	GeometryFactory factory = new GeometryFactory();
	private ArrayList<NEPPoint> points;
	private ArrayList<SimpleFeatureCollection> pointCollections = new ArrayList<SimpleFeatureCollection>();
	private Point TOWNHALL = factory.createPoint(new Coordinate(724493.899, 6175709.484));

	public GenerateNonExperiencePoints(String polylinefilename, int measuredistance) {
		polyLineFilename = polylinefilename;
		measureDistance = measuredistance;
	}

	private void run() throws IOException {
		polyinefile = new File(polyLineFilename);
		loadPolylines(polyinefile);
		Map<String,Serializable> connectParameters = new HashMap<String,Serializable>();
		connectParameters.put("url", polyinefile.toURI().toURL());
		// connectParameters.put("create spatial index", true );
		DataStore dataStore = DataStoreFinder.getDataStore(connectParameters);

		String[] typeNames = dataStore.getTypeNames();
		String typeName = typeNames[0];

		// logger.info("Reading content " + typeName);

		FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
		FeatureIterator<SimpleFeature> iterator;

		featureSource = dataStore.getFeatureSource(typeName);
		collection = featureSource.getFeatures();
		iterator = collection.features();
		points = new ArrayList<NEPPoint>();

		while (iterator.hasNext()) {
			SimpleFeature sf = iterator.next();
			MultiLineString ls = (MultiLineString) sf.getDefaultGeometry();

			System.out.println(sf);

			Object xx = sf.getAttribute("respondent");

			int sourceRouteID = (Integer) (Integer) sf.getAttribute("sr");
			int respondent = (Integer) sf.getAttribute("respid");

			Coordinate[] cs = ls.getCoordinates(); // the coordinates of the polyline
			int nCoord = cs.length;	// number of coordinates

			
			ArrayList<Coordinate> coordinatesList = new ArrayList<Coordinate>(Arrays.asList(cs));  
			Coordinate origin = coordinatesList.get(0);
			Coordinate destination = coordinatesList.get(coordinatesList.size()-1);
			
			// we build a line from Origin to Destination
			Coordinate[] cOD = new Coordinate[2];
			cOD[0] = origin;
			cOD[1] = destination;

			LineString originDestination = factory.createLineString(cOD);
			
			double l = 0;			// length on the current polyline segment

			for (int i=1; i<nCoord; i++) {
				Coordinate pt0 = cs[i-1];
				Coordinate pt1 = cs[i];
				double distance = pt0.distance(pt1);
				double deltaX = (pt1.x - pt0.x) / distance;
				double deltaY = (pt1.y - pt0.y) / distance;

				while (l <= distance) {	// loop up to the end of the current segment
					//					pcounter++;

					double newX = pt0.x + l*deltaX;
					double newY = pt0.y + l*deltaY;
					Coordinate c1 = new Coordinate(newX, newY);

					NEPPoint np = new NEPPoint(factory.createPoint(c1), respondent);

					Coordinate[] cSegment = {pt0, pt1};
					
					np.setSegment(factory.createLineString(cSegment));
					np.setOriginDestination(originDestination);
					np.setOD(originDestination);
					np.setRouteID(sourceRouteID);

					points.add(np);

					l += measureDistance;
				}
				l -= distance;	// = remaining distance on the next segment 
			}

		}


	}

	private void writeFeaturesToShapefile(String filename) throws IOException, SchemaException {

		SimpleFeatureType TYPE = DataUtilities.createType(
				"Location",                   // <- the name for our feature type
				"location:Point:srid=4400," + // <- the geometry attribute: Point type
				"orouteFID:Integer," + 
				"opointFID:Integer," + 			// the ID of the original point 
				"respondent:Integer," +			// the respondentid
				"distance:Double," + 			// Distance between point and it's projection on the route
				"seg_angle:Double," +           // <- a Double attribute
				"dst_twnh:Double," + 			// distance to townhall in meters
				"ang_twnh:Double," + 			// angle from segment towards townhall
				"dist_drct:Double," + 			// distance from point to the direct line
				"experience:Boolean"			// set true if an experience point
				);

		FeatureCollection<SimpleFeatureType, SimpleFeature> pointCollection;
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
		SimpleFeatureCollection collection = FeatureCollections.newCollection();

		for (NEPPoint n: points) {

			Point p1 = n.getSegment().getStartPoint();
			Point p2 = TOWNHALL;

			Coordinate[] pTownhall = {p1.getCoordinate(), p2.getCoordinate()};

			LineString towardsTownhall = factory.createLineString(pTownhall);

			Double angleToTownhall = getAngle(towardsTownhall,
					n.getSegment()
					);

			// Point nearLocation = getNearLocation(n.getGeometry(), p);

			// Double dist_drct = nearLocation.distance(originDestination);


			Point geom = n.getGeometry();

			
			featureBuilder.add(n.getSourceRouteID());
			featureBuilder.add(n.getRouteID());							// o routeFID
			featureBuilder.add(0);
			featureBuilder.add(n.getRespondentid());
			featureBuilder.add(0.0f);						// distance to polyline, here its null as we are on the polyline
			featureBuilder.add(n.getODAngle()); 			// TODO compares the current segment to the OD line
			featureBuilder.add(geom.distance(p2));			// distance to townhall
			featureBuilder.add(angleToTownhall);			// angle to townhall
			featureBuilder.add(geom.distance(n.getOD()));					// snap distance to the OD line
			featureBuilder.add(false);
			
			SimpleFeature pointFeature = featureBuilder.buildFeature(null);
			pointFeature.setDefaultGeometry(geom);
			
			collection.add(pointFeature);

		}

		pointCollections.add(collection);


		File newFile = new File(filename);
		// File newFile = getNewShapeFile(file);

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", newFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(TYPE);

		// You can comment out this line if you are using the createFeatureType method (at end of
		// class file) rather than DataUtilities.createType
		newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

		Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(collection);
				transaction.commit();
			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();
			} finally {
				transaction.close();
			}
			//System.exit(0); // success!
			System.out.println("FILE written");
		} else {
			// logger.error(typeName + " does not support read/write access");
			//System.exit(1);	// exit program with status 1 (error)
		}


	}


	public void loadPolylines(File polylinefilename) throws IOException {
		Map<String,Serializable> connectParameters = new HashMap<String,Serializable>();
		connectParameters.put("url", polylinefilename.toURI().toURL());
		// connectParameters.put("create spatial index", true );
		DataStore dataStore = DataStoreFinder.getDataStore(connectParameters);

		String[] typeNames = dataStore.getTypeNames();
		polylineTypeName = typeNames[0];
		polylineSource = dataStore.getFeatureSource(polylineTypeName);
	}


	/**
	 * 
	 * Returns the angle between two vectors
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	public double getAngle(LineString first,
			LineString second) {

		Coordinate first0 = first.getCoordinateN(0);
		Coordinate first1 = first.getCoordinateN(1);

		double ax = first1.x - first0.x;
		double ay = first1.y - first0.y;

		Coordinate second0 = second.getCoordinateN(0);
		Coordinate second1 = second.getCoordinateN(1);

		double bx = second1.x - second0.x;
		double by = second1.y - second0.y;

		return Math.abs(Math.atan2( ax*by - ay*bx, ax*bx + ay*by) *180/Math.PI);

		// return Math.acos(ax*bx + ay*by);

	}

	/**
	 * @param args
	 * @throws SchemaException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws SchemaException, IOException {



		String polylinefilename = "/Users/besn/Dropbox/Bikeability/CopenhagenExperiencePoints/4-RoutesManuallyCorrected/routesManuallyCorrected.shp";
		String outfilename = "/Users/besn/Dropbox/Bikeability/CopenhagenExperiencePoints/7-NonExperiencePointsLaidOut/NonExperiencePointsLaidOut.shp";
		int measureDistance = 25; // distance between the points

		GenerateNonExperiencePoints nep = new GenerateNonExperiencePoints(polylinefilename, measureDistance);
		nep.run();
		nep.writeFeaturesToShapefile(outfilename);


	}


}
