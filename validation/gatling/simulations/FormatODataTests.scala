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

import io.gatling.core.Predef._
import io.gatling.core.feeder.FeederBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.util.concurrent.ThreadLocalRandom

class FormatODataTests extends Simulation {

	// configuration
	// with dummy browser informations
	val httpConf = http
			.baseURL("http://"+sys.env("GO_DHUS_HOST_IP")+ ":" + sys.env("GO_DHUS_HOST_PORT") + "/odata/v1")
	    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
	    .doNotTrackHeader("1")
	    .acceptLanguageHeader("en-US,en;q=0.5")
	    .acceptEncodingHeader("gzip, deflate")
	    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
	    .basicAuth("root", "rootpassword") // create user root on centos1 dhus

	val sets = csv("entitysets.csv").queue // contains the names of entity sets

	// browse all formats available on all entity sets
	object Formats {
		val browse = exec(http("Service")
				.get("/").check(status.is(200)))
			.pause(1)
			.feed(sets)
			.exec(http("Entity Set ${entitySetName} XML format")
				.get("/${entitySetName}?$format=application/xml").check(status.is(200)))
			.exec(http("Entity Set ${entitySetName} JSON format")
				.get("/${entitySetName}?$format=application/json").check(status.is(200)))
			.exec(http("Entity Set ${entitySetName} CSV format")
				.get("/${entitySetName}?$format=text/csv").check(status.is(200)))
	}

	val browseEntitySets = scenario("OData Formats").exec(Formats.browse)

	setUp(
    	browseEntitySets.inject(rampUsers(11) over (11 seconds)) // scenario will be executed i times over n seconds
  	).assertions(global.failedRequests.percent.is(0))
  	.protocols(httpConf)
}