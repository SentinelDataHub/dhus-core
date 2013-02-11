/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2019,2020 GAEL Systems
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
package org.dhus.migration;

import java.sql.SQLException;
import java.util.Collection;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;

/**
 * A SpringLiquibase that checks that the externalised DB is up-to-date.
 * If the DB is embedded, it just does the migration.
 */
public class LiquibaseValidation extends SpringLiquibase
{
   protected boolean embedded = false;

   public boolean isEmbedded()
   {
      return embedded;
   }

   public void setEmbedded(boolean embedded)
   {
      this.embedded = embedded;
   }

   @Override
   public void afterPropertiesSet() throws LiquibaseException
   {
      if (embedded) // do the usual migration...
      {
         super.afterPropertiesSet();
         return;
      }
      try
      {
         Liquibase liquibase = createLiquibase(getDataSource().getConnection());
         try
         {
            liquibase.validate();

            Contexts ctxs = new Contexts(contexts);
            LabelExpression lblExp = new LabelExpression(labels);
            Collection<RanChangeSet> unexpectedChangeSets = liquibase.listUnexpectedChangeSets(ctxs, lblExp);
            Collection<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets(ctxs, lblExp);

            unexpectedChangeSets.forEach(cs -> log.info("Unexpected ChangeSet: " + cs.toString()));
            unrunChangeSets.forEach(cs -> log.info("Unrun ChangeSet: " + cs.toString(true)));

            if (!unexpectedChangeSets.isEmpty() || !unrunChangeSets.isEmpty())
            {
               throw new LiquibaseException("Unexpected or unrun ChangeSets");
            }
         }
         catch (LiquibaseException ex)
         {
            log.severe("DHuS DataBase schema mismatch ... Are you running the correct DHuS version?"
                  + " If yes, please run the DataBaseMigrationTool");
            throw ex;
         }
      }
      catch (SQLException ex)
      {
         throw new DatabaseException(ex);
      }
   }

}
