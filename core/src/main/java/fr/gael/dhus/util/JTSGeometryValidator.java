/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import java.util.Objects;

/**
 * Validates a JTS Geometry.
 */
public interface JTSGeometryValidator
{
   /**
    * Validate a Geometry, conditions are set in implementations.
    * @param to_check Geometry to validate.
    * @return {@code true} if the given Geometry is valid.
    */
   boolean validate(Geometry to_check);

   /**
    * An abstract class to create validator that check geometries with a reference Geometry.
    * (This class may be moved to its own file).
    */
   public static final class WithReference
   {
      /**
       * A reference Geometry, never null.
       */
      private final Geometry reference;

      /**
       * Create a validator with a reference Geometry.
       *
       * @param reference Geometry, must not be null.
       * @throws NullPointerException if reference is null.
       */
      public WithReference(Geometry reference)
      {
         Objects.requireNonNull(reference, "reference parameter cannot be null");
         this.reference = reference;
      }


      /**
       * A disjoint validator with reference.
       */
      public class Disjoint implements JTSGeometryValidator
      {
         @Override
         public boolean validate(Geometry to_check)
         {
            return reference.disjoint(to_check);
         }
      }


      /**
       * A intersection validator with reference.
       */
      public class Intersects implements JTSGeometryValidator
      {
         @Override
         public boolean validate(Geometry to_check)
         {
            return reference.intersects(to_check);
         }
      }


      /**
       * An inclusion validator with reference (Reference contains to_check).
       */
      public class Contains implements JTSGeometryValidator
      {
         @Override
         public boolean validate(Geometry to_check)
         {
            return reference.contains(to_check);
         }
      }


      /**
       * An inclusion validator with reference (Reference is within to_check).
       */
      public class Within implements JTSGeometryValidator
      {
         @Override
         public boolean validate(Geometry to_check)
         {
            return reference.within(to_check);
         }
      }
   }

   /**
    * Validates Geometry serialized in Well Known Text (WKT) format.
    * (This class may be moved to its own file).
    */
   public static final class WKTAdapter
   {
      private static final WKTReader READER = new WKTReader();

      private final JTSGeometryValidator validator;

      public WKTAdapter(JTSGeometryValidator validator)
      {
         Objects.requireNonNull(validator);
         this.validator = validator;
      }

      public static Geometry WKTtoJTSGeometry(String wkt) throws ParseException
      {
         return READER.read(wkt);
      }

      public boolean validate(String wkt_to_check) throws ParseException
      {
         return this.validator.validate(WKTtoJTSGeometry(wkt_to_check));
      }
   }
}
