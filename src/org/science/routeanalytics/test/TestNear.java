package org.science.routeanalytics.test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.algorithm.EuclideanDistanceToPoint;
import com.vividsolutions.jump.algorithm.PointPairDistance;

public class TestNear {

	GeometryFactory factory = new GeometryFactory();
	
	public TestNear() {
		Coordinate[] cOD = new Coordinate[3];
		cOD[0] = new Coordinate(0,0);
		cOD[1] = new Coordinate(10,10);
		cOD[2] = new Coordinate(20,0);
		
		LineString line = factory.createLineString(cOD);
		Coordinate c = new Coordinate(10,0);
		
		System.out.println(getLocation(line, factory.createPoint(c)));
		
		
	}
	
	
	/*
	 * Determines the location from each feature in the input features to the nearest feature in the near features.
	 * Works only with nearFeatures that are points. 
	 */
	public Point getLocation(Geometry inputFeature, 
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	TestNear tn = new TestNear();
	
	}

}
