package org.science.routeanalytics;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
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
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.algorithm.EuclideanDistanceToPoint;
import com.vividsolutions.jump.algorithm.PointPairDistance;

/**
 * @author Bernhard Snizek
 *
 */

public class SnapExperiencePoints {

	/*
	 * 
	 * imports a shapefile containing routes and a shapefile containing (experience) points and throws out a shapefile containing snapped points with several metrics
	 * 
	 * orouteFID 	:	the FID of the route shapefile
	 * opointFID 	: 	the FID of the point shapefile
	 * respondent 	:   the ID of the "respondent"
	 * distance		:  	the distance between the original point and the snapped one
	 * seg_angle	:   the angle between the direct line connecting origin and destination of the route and the segment the point lies on
	 * dst_twnh		:	closest distance to townhall 
	 * ang_twnh		:	angle between the segment and the line from the point to the location of townhall
	 * dist_drct	: 	closest distance from 
	 */


	private SimpleFeatureSource pointSource;
	private SimpleFeatureSource polylineSource;
	private String pointTypeName;
	private String polylineTypeName;
	private boolean doNEAR = true;

	GeometryFactory factory = new GeometryFactory();

	double SNAP_DISTANCE = 10;
	private SimpleFeatureBuilder featureBuilder;
	SimpleFeatureCollection pointCollection = null;
	private ArrayList<SimpleFeatureCollection> pointCollections = new ArrayList<SimpleFeatureCollection>();

	private static SimpleFeatureType outFileTYPE;

	private Point TOWNHALL = factory.createPoint(new Coordinate(724493.899, 6175709.484));

	public void loadPoints(File pointfilename) throws IOException {
		Map<String,Serializable> connectParameters = new HashMap<String,Serializable>();
		connectParameters.put("url", pointfilename.toURI().toURL());
		// connectParameters.put("create spatial index", true );
		DataStore dataStore = DataStoreFinder.getDataStore(connectParameters);

		String[] typeNames = dataStore.getTypeNames();
		pointTypeName = typeNames[0];

		// logger.info("Reading content " + typeName);

		pointSource = dataStore.getFeatureSource(pointTypeName);

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
	 * Returns a list of Point Features given the respondent ID
	 * 
	 * @param id
	 * @return
	 * @throws CQLException
	 * @throws IOException 
	 * 
	 */
	public SimpleFeatureCollection getPointsByID(Object id) throws CQLException, IOException {
		FeatureType schema = pointSource.getSchema();
		String name = schema.getGeometryDescriptor().getLocalName();

		String text = "WHERE respondent=" + id;

		Filter filter = CQL.toFilter(text);

		System.out.println(filter);

		Query query = new Query(pointTypeName, filter, new String[] { name });

		return pointSource.getFeatures(query);

	}

	/*
	 * Determines the location from each feature in the input features to the nearest feature in the near features.
	 * Works only with nearFeatures that are points. 
	 */
	public Point getNearLocation(Geometry inputFeature, 
			Geometry nearFeature) {



		Coordinate pointCoordinate = nearFeature.getCoordinate();
		PointPairDistance ppd = new PointPairDistance(); 
		EuclideanDistanceToPoint.computeDistance(inputFeature, pointCoordinate, ppd); 
		Coordinate resultcoord = null;

		for (Coordinate cc : ppd.getCoordinates()) { 
			// System.out.println(cc); 
			if (cc.equals(pointCoordinate) == false) {
				resultcoord = cc;
			}
		} 

		return factory.createPoint(resultcoord);

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


	public void importShpFiles(File polylinefile,
			File pointfilename) throws IOException, CQLException {

		loadPoints(pointfilename);
		loadPolylines(polylinefile);


		Map<String,Serializable> connectParameters = new HashMap<String,Serializable>();
		connectParameters.put("url", polylinefile.toURI().toURL());
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

		// let us get the schema and pop it into a simple list of strings
		SimpleFeatureType schema = featureSource.getSchema();
		List<AttributeDescriptor> ads = schema.getAttributeDescriptors();
		ArrayList<String> fieldnames = new ArrayList<String>();

		for (AttributeDescriptor ad : ads) {
			String ln = ad.getLocalName();
			if (ln != "geom") {
				fieldnames.add(ln);
			}
		}

		System.out.println(fieldnames);

		int numberOfPolylines = collection.size();
		int polylineCounter = 0;
		int pointCounter = 0;

		pointCollection = FeatureCollections.newCollection("internal");

		try {

			// let us loop over the polylines
			
			while (iterator.hasNext()) {

				polylineCounter++;

				System.out.println(polylineCounter + "/" + numberOfPolylines);

				SimpleFeature feature = iterator.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				// System.out.println(geometry);

				Coordinate[] coordinates = geometry.getCoordinates();
				ArrayList<Coordinate> coordinatesList = new ArrayList<Coordinate>(Arrays.asList(coordinates));  
				Coordinate origin = coordinatesList.get(0);
				Coordinate destination = coordinatesList.get(coordinatesList.size()-1);

				// double euclidianDistance = origin.distance(destination);

				// the euclidian distance comes here
				// System.out.println(euclidianDistance);

				// we build a line from Origin to Destination
				Coordinate[] cOD = new Coordinate[2];
				cOD[0] = origin;
				cOD[1] = destination;

				LineString originDestination = factory.createLineString(cOD);

				// let us chop this thing into segments

				Long respondent = new Double((Double) feature.getAttribute("respondent")).longValue();

				// System.out.println("RESPONDENTID: " + respondent);
				String polylineGeotoolsID = feature.getID();

				// System.out.println(polylineGeotoolsID);

				int polylineFID = new Integer(Arrays.asList(polylineGeotoolsID.split("\\.")).get(1)) -1 ;

				// get all points belonging to the polyline
				SimpleFeatureCollection points = getPointsByID(respondent);

				System.out.println("Number of points: " + points.size());

				ArrayList<LineString> segments = getSegments(coordinatesList);

				Iterator<SimpleFeature> pIterator = points.iterator();

				while (pIterator.hasNext()) {
					pointCounter++;
					SimpleFeature sf = pIterator.next();

					String pointGeotoolsID = sf.getID();

					int pointFID = new Integer(Arrays.asList(pointGeotoolsID.split("\\.")).get(1)) -1 ;

					Point p = (Point) sf.getDefaultGeometry();
					Point nearLocation = null;

					if (doNEAR) {
						nearLocation = getNearLocation((Geometry)feature.getDefaultGeometry(),p);
					} else {
						// point already on polyline
						nearLocation = p;
					}

					LineString nearSegment = null;

					for (LineString ls :segments) {
						//System.out.println(ls.distance(nearLocation));
						if (ls.distance(nearLocation) < SNAP_DISTANCE) {
							nearSegment = ls;
						}
					}

					if (nearSegment != null) {

						// System.out.println(nearSegment);
						Double angle = getAngle(originDestination,
								nearSegment
								);

						Point p1 = nearSegment.getStartPoint();
						Point p2 = TOWNHALL;

						Coordinate[] pTownhall = {p1.getCoordinate(), p2.getCoordinate()};

						LineString towardsTownhall = factory.createLineString(pTownhall);

						Double angleToTownhall = getAngle(towardsTownhall,
								nearSegment
								);


						Double dist_townhall = nearLocation.distance(TOWNHALL);

						// distance to the direct line
						Double dist_drct = nearLocation.distance(originDestination);


						featureBuilder.add(nearLocation);
						featureBuilder.add(polylineFID);
						featureBuilder.add(pointFID);
						featureBuilder.add(respondent);
						featureBuilder.add(p.distance((Geometry) feature.getDefaultGeometry()));
						featureBuilder.add(angle);
						featureBuilder.add(dist_townhall);
						featureBuilder.add(angleToTownhall);
						featureBuilder.add(dist_drct);
						featureBuilder.add(true);

						SimpleFeature pointFeature = featureBuilder.buildFeature(null);
						pointCollection.add(pointFeature);
						if (pointCounter == 5000) {
							pointCounter = 0;
							pointCollections.add(pointCollection);
							pointCollection = FeatureCollections.newCollection("internal");
						}
					} else {
						System.out.println("B¯H");

					}

				}

			}
		} finally {}
		pointCollections.add(pointCollection);




	}
	public ArrayList<LineString> getSegments(ArrayList<Coordinate> coordinatesList) {
		ArrayList<LineString> lss = new ArrayList<LineString>();

		for (int i=0;i<coordinatesList.size()-1; i++) {
			// System.out.println("Segment #" + i);
			// build the segment
			Coordinate cFrom =  coordinatesList.get(i);
			Coordinate cTo = coordinatesList.get(i+1);
			Coordinate[] c = new Coordinate[2];
			c[0] = cFrom;
			c[1] = cTo;

			// System.out.println(cFrom + " " + cTo);

			LineString ls = factory.createLineString(c);
			lss.add(ls);
		}
		return lss;

	}



	private void initializeFeatureCollection() throws SchemaException {

		pointCollection = FeatureCollections.newCollection();
		featureBuilder = new SimpleFeatureBuilder(outFileTYPE);
	}

	private void writeFeaturesToShapefile(String filename) throws IOException {

		File newFile = new File(filename);

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", newFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(outFileTYPE);

		/*
		 * You can comment out this line if you are using the createFeatureType
		 * method (at end of class file) rather than DataUtilities.createType
		 */
		newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);


		/*
		 * Write the features to the shapefile
		 */



		DefaultTransaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

			for (FeatureCollection f: pointCollections) {


				featureStore.setTransaction(transaction);
				try {

					System.out.println("SIZE: " + pointCollection.size());

					featureStore.addFeatures(f);
					transaction.commit();

				} catch (Exception problem) {
					problem.printStackTrace();
					transaction.rollback();

				} 

			}


			transaction.close();

			System.out.println("FINISHED");
			System.exit(0); // success!
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}



	}

	public static void main(String[] args) throws IOException, CQLException, SchemaException {

		outFileTYPE = DataUtilities.createType(
				"Location",                   // <- the name for our feature type
				"location:Point:srid=4400," + // <- the geometry attribute: Point type
				"orouteFID:Integer," + 
				"opointFID:Integer," + 			// the ID of the original point 
				"respondent:Integer," +			// the respondentid
				"distance:Double," + 			// Distance between point and it's projection on the route
				"seg_angle:Double," +           // <- a Double attribute
				"dst_twnh:Double," + 			// distance to townhall in meters
				"ang_twnh:Double," + 			// angle from segment towards townhall
				"dist_drct:Double"						// distance from point to the direct line
				);

		String polylinefilename = "/Users/besn/Dropbox/Bikeability/CopenhagenExperiencePoints/4-RoutesManuallyCorrected/routesManuallyCorrected.shp";
		String pointfilename = "/Users/besn/Dropbox/Bikeability/CopenhagenExperiencePoints/5-PointsReclassified/pointsReclassified.shp";
		String resultfielname = "/Users/besn/Dropbox/Bikeability/CopenhagenExperiencePoints/6-PointsSnapped/SnappedExperiencePoints.shp";

		SnapExperiencePoints rc = new SnapExperiencePoints();

		rc.initializeFeatureCollection();

		rc.importShpFiles(new File(polylinefilename),
				new File(pointfilename));

		rc.writeFeaturesToShapefile(resultfielname);

	}

}


