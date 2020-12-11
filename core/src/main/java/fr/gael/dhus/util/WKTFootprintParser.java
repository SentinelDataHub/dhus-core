/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017-2019 GAEL Systems
 *
 * This file is part of DHuS software sources.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gael.dhus.util;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.OperatorContains;
import com.esri.core.geometry.OperatorDifference;
import com.esri.core.geometry.OperatorExportToWkt;
import com.esri.core.geometry.OperatorImportFromWkt;
import com.esri.core.geometry.OperatorIntersection;
import com.esri.core.geometry.OperatorIntersects;
import com.esri.core.geometry.OperatorUnion;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.WktExportFlags;
import com.esri.core.geometry.WktImportFlags;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class WKTFootprintParser
{

   private static final double  EAST_LIMIT =  180; //Right
   private static final double  WEST_LIMIT = -180; //Left
   private static final double NORTH_LIMIT =  85.05115; //Top
   private static final double SOUTH_LIMIT = -85.05115; //Bottom

   private static final int WKID_WGS84 = 4326;
   private static final SpatialReference WGS84 = SpatialReference.create(WKID_WGS84);

   private static final Logger LOGGER = LogManager.getLogger();

   /**
    * Check WKT Footprint validity.
    *
    * @param wkt to check
    * @return corrected WKT footprint or null
    */
   public static String reformatWKTFootprint(String wkt)
   {
      if (!wkt.contains("POLYGON")) // Other kinds of geometry don't need reformatting
      {
         return wkt;
      }

      Geometry original = OperatorImportFromWkt.local()
            .execute(WktImportFlags.wktImportDefaults, Geometry.Type.Unknown, wkt.replaceAll("(?<!\\d)180 ","179.999999 "), null);

      Geometry geometry = original;
//      LOGGER.info("wkt: {}",wkt);

      geometry = deletePointsOverThePoles(original);
//      String deletePointsOverThePoles=OperatorExportToWkt.local().execute(WktExportFlags.wktExportDefaults, geometry, null);
//      LOGGER.info("deletePointsOverThePoles: {}",deletePointsOverThePoles);

      geometry = addSplittingPointsOnEastWestBorder(geometry, original);
//      String addSplittingPointsOnEastWestBorder=OperatorExportToWkt.local().execute(WktExportFlags.wktExportDefaults, geometry, null);
//      LOGGER.info("addSplittingPointsOnEastWestBorder: {}",addSplittingPointsOnEastWestBorder);

      geometry = separateLoops(geometry);
//      String separateLoops=OperatorExportToWkt.local().execute(WktExportFlags.wktExportDefaults, geometry, null);
//      LOGGER.info("separateLoops: {}",separateLoops);

      geometry = trimToFrame(geometry);
//      String trimToFrame=OperatorExportToWkt.local().execute(WktExportFlags.wktExportDefaults, geometry, null);
//      LOGGER.info("trimToFrame: {}",trimToFrame);

      geometry = invertIfNecessary(geometry);
//      String invertIfNecessary=OperatorExportToWkt.local().execute(WktExportFlags.wktExportDefaults, geometry, null);
//      LOGGER.info("invertIfNecessary: {}",invertIfNecessary);

      return OperatorExportToWkt.local().execute(WktExportFlags.wktExportDefaults, geometry, null);
   }

   private static String startFromLimit(Geometry geometry)
   {
      String wkt = OperatorExportToWkt.local().execute(WktExportFlags.wktExportDefaults, geometry, null);

      wkt = cleanCoordinates(wkt);

      if (wkt == null)
      {
         return null;
      }

      wkt = wkt.trim();

      if (wkt.endsWith(","))
      {
         wkt=wkt.substring(0,wkt.length()-1);
      }

      return "POLYGON ((" + wkt + "))";
   }

   private static Geometry invertIfNecessary(Geometry geometry)
   {
      Polygon pol = ((Polygon) geometry);

      Point aux = new Point();

      int numPolygons = pol.getPathCount();
      List<Polygon> polygons = new ArrayList<Polygon>(numPolygons);
      for (int current = 0; current < numPolygons; current++)
      {
         int start = pol.getPathStart(current);
         int end = pol.getPathEnd(current);
         Polygon polygon = new Polygon();
         ((Polygon) geometry).getPoint(start, aux);
         polygon.startPath(aux);
         for (int i = start + 1; i < end; i++)
         {
            ((Polygon) geometry).getPoint(i, aux);
            polygon.lineTo(aux);
         }
         polygon.closePathWithLine();
         polygons.add(polygon);
      }

      int stepSize = 10;
      int tests = 360 / stepSize;//test 360 degrees, from -180 to 180.
      tests += 1; //To include the edge
      boolean[] testResults = new boolean[tests];

      for (Polygon p: polygons)
      {
         for (int i = 0; i < tests; i++)
         {
            int testX = -180 + i * stepSize;
            testResults[i] = testResults[i] || OperatorIntersects.local().execute(p, new Point(testX, 0), WGS84, null);
         }
      }

      int countTrue = 0;
      for (Boolean b: testResults)
      {
         if (b)
         {
            countTrue++;
         }
      }
      boolean result = countTrue >= tests / 2;

      if (result)
      {
         String wkt = "POLYGON ((-180 85.05115, -60 85.05115, 0 85.05115, 60 85.05115, 180 85.05115, 180 0, 180 -85.05115, 60 -85.05115, 0 -85.05115, -60 -85.05115, -180 -85.05115, -180 0, -180 85.05115))";
         Geometry original = OperatorImportFromWkt.local().execute(WktImportFlags.wktImportDefaults, Geometry.Type.Unknown, wkt, null);

         for (Polygon p: polygons)
         {
            original = OperatorDifference.local().execute(original, p, WGS84, null);
         }
         return original;
      }
      else
      {
         return geometry;
      }
   }

   private static boolean crossZero(Point prev, Point p)
   {
      double distanceInsidePolygon = Math.abs(prev.getX() - p.getX());

      boolean distanceIsRealistic = distanceInsidePolygon <= 120;

      if (!distanceIsRealistic)
      {
         return false;
      }

      Point pointNeg = null;
      Point pointPos = null;

      if (prev.getX() <= 0)
      {
         pointNeg = prev;
      }
      else if (prev.getX() >= 0)
      {
         pointPos = prev;
      }

      if (p.getX() <= 0)
      {
         pointNeg = p;
      }
      else if (p.getX() >= 0)
      {
         pointPos = p;
      }

      if (pointNeg == null || pointPos == null)
      {
         return false;
      }

      boolean isPrevValid = pointNeg.getX() > -60 && pointNeg.getX() <= 0;
      boolean isPValid = pointPos.getX() < 60 && pointPos.getX() >= 0;

      return isPrevValid && isPValid;
   }

   // This method was specially developed for SLSTR products when the self-intersection happens on the date line
   // This is an invalid polygon so an ad-hoc algorithm needs to exist, since the library does not support such polygons
   // The algorithm tries to process the invalid polygon and if it recognizes that the polygon is not an invalid polygon
   // it process it with the method separateLoopsValid which works on all valid polygons.
   private static Geometry separateLoops(Geometry geometry)
   {
      try
      {
         Polygon original = (Polygon) (geometry.copy());
         LinkedList<Polygon> polygons = new LinkedList<>();

         final int pointsToProcess = original.getPointCount();

         // Search for the points where there is a crossing from negative coordinates to positive coordinates around x=0, not the date line
         Point first = null;
         Point prev = original.getPoint(pointsToProcess - 1);
         int firstPosition = 0;
         for (; firstPosition < pointsToProcess - 1; firstPosition++)
         {
            Point p = original.getPoint(firstPosition);
            if (prev.getX() < p.getX() && crossZero(prev, p))
            {
               first = p;
               prev = p;
               break;
            }
            else
            {
               prev = p;
            }
         }

         if (first == null)
         {
            return separateLoopsValid(original);
         }

         Polygon current = new Polygon();
         polygons.add(current);
         current.startPath(first);

         int phase = 0;

         int phase0=1;
         int phase1=0;
         int phase2=0;
         int phase3=0;

         int indexPoint = firstPosition;
         for (int counter = 0; counter < pointsToProcess; counter++)
         {
            indexPoint = (indexPoint + 1) % pointsToProcess;

            Point p = original.getPoint(indexPoint);

            switch (phase)
            {
               case (0):
               {
                  if (cross(prev, p, original))
                  {
                     phase = 1;
                     phase1++;
                     current = startNewPolygon(polygons, p);
                  }
                  else
                  {
                     if (crossZero(prev, p))
                     {
                        phase = 2;
                        phase2++;
                        current.closePathWithLine();
                        current = startNewPolygon(polygons, prev);
                        current.lineTo(p);
                     }
                     else
                     {
                        current.lineTo(p);
                     }
                  }
                  prev = p;
                  continue;
               }
               case (1):
               {
                  if (cross(prev, p, original))
                  {
                     phase = 0;
                     phase0++;
                     current.closePathWithLine();
                     current = polygons.get(0);
                     current.lineTo(p);
                  }
                  else
                  {
                     current.lineTo(p);
                  }
                  prev = p;
                  continue;
               }
               case (2):
               {
                  if (cross(prev, p, original))
                  {
                     phase = 3;
                     phase3++;
                     current = startNewPolygon(polygons, p);
                  }
                  else
                  {
                     if (crossZero(prev, p))
                     {
                        current.lineTo(first);
                        current.closePathWithLine();
                        break;
                     }
                     else
                     {
                        current.lineTo(p);
                     }
                  }
                  prev = p;
                  continue;
               }
               case (3):
               {
                  if (cross(prev, p, original))
                  {
                     phase = 2;
                     phase2++;
                     current.closePathWithLine();
                     current = polygons.get(2);
                     current.lineTo(p);
                  }
                  else
                  {
                     current.lineTo(p);
                  }
                  prev = p;
                  continue;
               }
               default:
                  return separateLoopsValid(geometry);
            }

         }

         if (polygons.size() != 4 || phase0 != 2 || phase1 != 1 || phase2 != 2 || phase3 != 1)
         {
            return separateLoopsValid(geometry);
         }

         Geometry resultPartialRight = OperatorUnion.local().execute(polygons.get(0), polygons.get(3), WGS84, null);

         Geometry resultPartialLeft = OperatorUnion.local().execute(polygons.get(1), polygons.get(2), WGS84, null);

         return OperatorUnion.local().execute(resultPartialRight, resultPartialLeft, WGS84, null);
      }
      catch (Exception e)
      {
         return separateLoopsValid(geometry);
      }
   }

   private static Polygon startNewPolygon(LinkedList<Polygon> polygons, Point p)
   {
      Polygon current;
      current = new Polygon();
      polygons.add(current);
      current.startPath(p);
      return current;
   }

   private static Geometry deletePointsOverThePoles(Geometry geometry)
   {
      Polygon originalPolygon = (Polygon) geometry;
      Point prev, next, current;
      Polygon newPolygon = null;
      for (int i = 0; i < originalPolygon.getPointCount(); i++)
      {
         current = originalPolygon.getPoint(i);

         if (i == originalPolygon.getPointCount() - 1) // last point of the polygon
         {
            next = originalPolygon.getPoint(0);
         }
         else
         {
            next = originalPolygon.getPoint(i + 1);
         }

         if (i == 0) // first point of the polygon
         {
            prev = originalPolygon.getPoint(originalPolygon.getPointCount() - 1);
         }
         else
         {
            prev = originalPolygon.getPoint(i - 1);
         }

         if (outsideLimits(current) && outsideLimits(prev) && outsideLimits(next))
         {
            continue;
         }

         boolean currentInside = !outsideLimits(current);
         boolean nextInside = !outsideLimits(next);
         boolean prevInside = !outsideLimits(prev);

         if (currentInside)
         {
            newPolygon = addPointToPolygon(current, newPolygon);
         }
         else
         {
            // current is outside
            // you only arrive here if either the next or the previous are outside
            if (prevInside)
            {
               newPolygon = addPointToPolygon(middlePoint(prev, current, geometry), newPolygon);
            }
            if (nextInside)
            {
               newPolygon = addPointToPolygon(middlePoint(next, current, geometry), newPolygon);
            }
         }
      }
      newPolygon.closePathWithLine();

      return newPolygon;
   }

   // This method is called if in the original polygon there is a line between a point inside the projection limits and other outside.
   // This method calculates the coordinates of the point exactly on the limit and inside the line between the original two points
   private static Point middlePoint(Point p1, Point p2, Geometry geo)
   {
      double yCoordinate = 0;
      if (p1.getY() > NORTH_LIMIT || p2.getY() > NORTH_LIMIT)
      {
         yCoordinate = NORTH_LIMIT;
      }
      else if (p1.getY() < SOUTH_LIMIT || p2.getY() < SOUTH_LIMIT)
      {
         yCoordinate = SOUTH_LIMIT;
      }

      double x1, x2, y1, y2;

      x2 = p2.getX();
      x1 = p1.getX();
      y2 = p2.getY();
      y1 = p1.getY();

      if (cross(p1, p2, geo))
      {
         // The following calculations serve to calculate the Y coordinate where the polygon crosses the line of 180
         // we first convert the negative x to the positive value greater than 180 to use the same referential
         // change negative x to positive x
         if (p2.getX() < 0)
         {
            x2 = EAST_LIMIT + (EAST_LIMIT - Math.abs(p2.getX()));
            x1 = p1.getX();
            y2 = p2.getY();
            y1 = p1.getY();
         }
         else // p/getX()<0
         {
            x2 = EAST_LIMIT + (EAST_LIMIT - Math.abs(p1.getX()));
            x1 = p2.getX();
            y2 = p1.getY();
            y1 = p2.getY();
         }
      }

      if (x1 == x2)
      {
         return new Point(x1, yCoordinate);
      }
      // y=mx+b
      // m=(y2-y1)/(x2-x1)
      // b=y-mx
      double m = (y2 - y1) / (x2 - x1);
      double b = y1 - (m * x1);

      //y=mx+b <=> y-b = mx <=> (y-b)/m = x
      double newX = (yCoordinate - b) / m;
      if (newX > 180)
      {
         newX = -180 + (newX - 180);
      }
      else if (newX < -180)
      {
         newX = 180 - (newX + 180);
      }

      return new Point(newX, yCoordinate);
   }

   private static Polygon addPointToPolygon(Point point, Polygon polygon)
   {
      if (polygon == null)
      {
         polygon = new Polygon();
         polygon.startPath(point.getX(), point.getY());
      }
      else
      {
         polygon.lineTo(point.getX(), point.getY());
      }

      return polygon;
   }

   private static boolean outsideLimits(Point point)
   {
      return point.getY() >= NORTH_LIMIT || point.getY() <= SOUTH_LIMIT;
   }

   private static Geometry trimToFrame(Geometry geometry)
   {
      Envelope env = new Envelope(WEST_LIMIT, SOUTH_LIMIT, 0, NORTH_LIMIT);
      final Geometry left = doIntersection(geometry, env);
      env = new Envelope(0, SOUTH_LIMIT, EAST_LIMIT, NORTH_LIMIT);
      final Geometry right = doIntersection(geometry, env);
      return OperatorUnion.local().execute(left, right, WGS84, null);
   }

   private static Geometry separateLoopsValid(Geometry geometry)
   {

      String polygon = startFromLimit(geometry);

      if (polygon == null)
      {
         return geometry;
      }

      LinkedList<Polygon> polygons = new LinkedList<>();

      Polygon original = ((Polygon) OperatorImportFromWkt.local().execute(WktImportFlags.wktImportDefaults, Geometry.Type.Unknown, polygon, null));

      boolean buildingLoop = false;
      if (original.getPoint(0).getX() + original.getPoint(1).getX() != 0)
      {
         original.reverseAllPaths();
      }
      Point first = original.getPoint(0);
      Point loopFirst;
      Point prev = first;

      Polygon current = new Polygon();
      polygons.add(current);
      current.startPath(first);
      for (int i = 1; i < original.getPointCount(); i++)
      {
         Point p = original.getPoint(i);
         if (p.getX() + prev.getX() == 0)
         {
            if (buildingLoop)
            {
               current.closePathWithLine();
               buildingLoop = false;
               current = polygons.get(0);
            }
            else
            {
               String splitterCoordinates = " " + (int) p.getX() + " " + p.getY();
               final String[] nextPoints = polygon.split(splitterCoordinates, 2);
               if (nextPoints.length == 2)
               {
                  polygon = nextPoints[1];
                  final String[] split = nextPoints[1].split(" " + (int) p.getX() + " -?\\d+\\.?\\d*, " + (int) prev.getX(), 2);
                  if (split.length == 2 && (!split[0].contains(" 180 ") && !split[0].contains(" -180 ")))
                  {
                     current = new Polygon();
                     polygons.add(current);
                     buildingLoop = true;
                     loopFirst = p;
                     current.startPath(loopFirst);
                     prev = p;
                     continue;
                  }
               }
            }
         }
         current.lineTo(p);
         prev = p;
      }
      current.lineTo(first);

      Geometry result = polygons.get(0);

      for (int i = 1; i < polygons.size(); i++)
      {
         result = OperatorUnion.local().execute(result, polygons.get(i), WGS84, null);
      }

      return result;
   }

   private static String cleanCoordinates(String polygon)
   {
      int firstCoordinateIndex = 0;
      while (firstCoordinateIndex < polygon.length() && !Character.isDigit(polygon.charAt(firstCoordinateIndex)) && (polygon.charAt(firstCoordinateIndex) != '-'))
      {
         firstCoordinateIndex++;
      }
      int lastCoordinateIndex = polygon.length() - 1;
      while (lastCoordinateIndex > firstCoordinateIndex && !Character.isDigit(polygon.charAt(lastCoordinateIndex)))
      {
         lastCoordinateIndex--;
      }
      lastCoordinateIndex++;
      polygon = polygon.substring(firstCoordinateIndex, lastCoordinateIndex);

      final String positiveLoop = "180 -?(?!85.05115)\\d+.?\\d*, -180 -?\\d+.?\\d*,( -?(?!180)\\d+.?\\d* -?\\d+.?\\d*,)+? -180 -?(?!85.05115)\\d+.?\\d*, 180 -?\\d+.?\\d*";
      String[] splitCoordinates = polygon.split(positiveLoop,2);
      if (splitCoordinates.length == 2)
      {
         return polygon.substring(splitCoordinates[0].length()) + "," + splitCoordinates[0];
      }
      final String negativeLoop = "-180 -?(?!85.05115)\\d+.?\\d*, 180 -?\\d+.?\\d*,( -?(?!180)\\d+.?\\d* -?\\d+.?\\d*,)+? 180 -?(?!85.05115)\\d+.?\\d*, -180 -?\\d+.?\\d*";
      splitCoordinates = polygon.split(negativeLoop,2);
      if (splitCoordinates.length == 2)
      {
         return polygon.substring(splitCoordinates[0].length()) + "," + splitCoordinates[0];
      }

      splitCoordinates = polygon.split(" ?-180 -?\\d+\\.?\\d*, 180 -?\\d+\\.?\\d*,", 2);

      if (splitCoordinates.length != 2)
      {
         return null;
      }

      int splitCoordinatesLen = splitCoordinates[0].length();

      polygon = polygon.substring(splitCoordinatesLen) + ", " + splitCoordinates[0].substring(0, splitCoordinatesLen - 1);

      String firstCoordinate = polygon.split(",", 2)[0].trim();
      String lastCoordinate = polygon.substring(polygon.lastIndexOf(",") + 1).replaceAll("[)]", "").trim();

      if (!firstCoordinate.equals(lastCoordinate))
      {
         polygon = polygon.replace(lastCoordinate, lastCoordinate + ", " + firstCoordinate);
      }
      return polygon;
   }

   private static Geometry doIntersection(Geometry geometry, Envelope side)
   {
      return OperatorIntersection.local().execute(geometry, side, WGS84, null);
   }

   private static Geometry addSplittingPointsOnEastWestBorder(Geometry geometry, Geometry original)
   {
      Polygon polygon = (Polygon) geometry.copy();

      List<Double> pointToAdd = pairYCrossings(polygon, original);

      return addPoints(polygon, pointToAdd, original);
   }

   private static Polygon addPoints(Polygon polygon, List<Double> pointToAdd, Geometry geo)
   {
      Point prev = null;
      Point first = null;
      Polygon result = new Polygon();
      for (int i = 0; i < polygon.getPointCount(); i++)
      {
         Point p = polygon.getPoint(i);
         if (prev == null)
         {
            prev = p;
            first = p;
            result.startPath(new Point(p.getX(), p.getY()));
         }
         else
         {
            if (cross(p, prev, geo))
            {
               addMidPoints(p, prev, result, pointToAdd);
            }
            result.lineTo(p.getX(), p.getY());
            prev = p;
         }

      }
      if (cross(first, prev, geo))
      {
         addMidPoints(first, prev, result, pointToAdd);
      }
      return result;
   }

   // This method checks if an additional point needs to be added on the date line.
   // If the number of times the polygon crosses the dateline is not even, this means an additional point needs to be added
   // This point is added either at the NORTH LIMIT or the SOUTH LIMIT
   private static List<Double> pairYCrossings(Polygon polygon, Geometry original)
   {

      List<Double> yList = new ArrayList<>();
      Point prev = null;
      Point first = null;
      for (int i = 0; i < polygon.getPointCount(); i++)
      {
         Point p = polygon.getPoint(i);
         if (prev == null)
         {
            prev = p;
            first = p;
         }
         else
         {
            if (cross(p, prev, original))
            {
               double newY = getNewY(p, prev);
               yList.add(newY);
            }

            prev = p;
         }
      }

      if (cross(first, prev, original))
      {
         yList.add(getNewY(first, prev));
      }

      if (yList.size() % 2 != 0) // this means the amount of crossings is not matched, so we need to match also with one of the limits
      {
         Collections.sort(yList);
         double addSouth = Math.abs(SOUTH_LIMIT - yList.get(0));
         for (int i = 1; i < yList.size(); i += 2)
         {
            addSouth += Math.abs(yList.get(i) - yList.get(i + 1));
         }

         double addNorth = 0;
         for (int i = 0; i < yList.size() - 1; i += 2)
         {
            addNorth += Math.abs(yList.get(i) - yList.get(i + 1));
         }
         addNorth += Math.abs(NORTH_LIMIT - yList.get(yList.size() - 1));

         if (addSouth < addNorth)
         {
            return Arrays.asList(SOUTH_LIMIT, yList.get(0));
         }
         else
         {
            return Arrays.asList(NORTH_LIMIT, yList.get(yList.size() - 1));
         }
      }
      return new ArrayList<>();
   }

   private static boolean cross(Point x, Point x1, Geometry original)
   {
      double distanceInsidePolygon = Math.abs(x1.getX() - x.getX());
      double distanceThroughDateLine = 360 - distanceInsidePolygon;

      // If the distance is too close to the middle (distance from a point to itself around the world is 360. half is 180) Then I gave 10 units buffer.
      if (distanceInsidePolygon > 170 && distanceInsidePolygon < 190)
      {
         return OperatorContains.local().execute(original, new Point(180, NORTH_LIMIT), WGS84, null);
      }

      // boolean areBothOutsideLimits = (x1.getY()>NORTH_LIMIT && x.getY()>NORTH_LIMIT) || (x1.getY()<SOUTH_LIMIT && x.getY()<SOUTH_LIMIT);
      return distanceThroughDateLine < distanceInsidePolygon;
   }

   private static void addMidPoints(Point p, Point prev, Polygon result, List<Double> pointToAdd)
   {
      final double newY = getNewY(p, prev);

      // the values bellow are done so if we cross from east to west first we add the east point and then the west or vice-versa
      int newXOne = (int) EAST_LIMIT;
      int newXTwo = (int) WEST_LIMIT;

      if (p.getX() > prev.getX())
      {
         newXOne = (int) WEST_LIMIT;
         newXTwo = (int) EAST_LIMIT;
      }

      if (prev.getX() != newXOne)
      {
         result.lineTo(newXOne, newY);
      }
      if (pointToAdd.size() > 0 && pointToAdd.get(1).equals(newY))
      {
         if (pointToAdd.get(0) != newY)
         {
            result.lineTo(newXOne, pointToAdd.get(0));
            result.lineTo(newXTwo, pointToAdd.get(0));
         }
      }
      if (p.getX() != newXTwo)
      {
         result.lineTo(newXTwo, newY);
      }
   }

   private static double getNewY(Point p, Point prev)
   {
      double x2, x1, y2, y1;

      // The following calculations serve to calculate the Y coordinate where the polygon crosses the line of 180
      // we first convert the negative x to the positive value greater than 180 to use the same referential
      // change negative x to positive x
      if (prev.getX() < 0)
      {
         x2 = EAST_LIMIT + (EAST_LIMIT - Math.abs(prev.getX()));
         x1 = p.getX();
         y2 = prev.getY();
         y1 = p.getY();
      }
      else // p/getX()<0
      {
         x2 = EAST_LIMIT + (EAST_LIMIT - Math.abs(p.getX()));
         x1 = prev.getX();
         y2 = p.getY();
         y1 = prev.getY();
      }

      // y=mx+b
      // m=(y2-y1)/(x2-x1)
      // b=y-mx
      double m = (y2 - y1) / (x2 - x1);
      double b = y1 - (m * x1);

      double newY = m * EAST_LIMIT + b;
      if (newY > NORTH_LIMIT)
      {
         return NORTH_LIMIT;
      }
      if (newY < SOUTH_LIMIT)
      {
         return SOUTH_LIMIT;
      }

      return newY;
   }
}
