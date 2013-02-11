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

class FilterODataTests extends Simulation {

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

	/* 
	 TODO 
	 	- add feeders that provide filter arguments
	 	- use operator filter in scenario
	*/

	sealed class EntitySet protected (val entitySetName : String, val properties : FeederBuilder[_], val propertyCount : Int) {
		val filter = repeat(propertyCount) {
			exec(http("Service")
				.get("/").check(status.is(200)))
			.pause(1)
			.feed(properties)
			.exec(http("Filter "+entitySetName+" ${property} Eq")
				.get("/"+entitySetName+"?$filter=${property} eq ${eqValue}").check(status.is(200)))
			.pause(1)
			.exec(http("Filter "+entitySetName+" ${property} Ne")
				.get("/"+entitySetName+"?$filter=${property} ne ${neValue}").check(status.is(200)))
			.pause(1)
			.exec(http("OrderBy "+entitySetName+" ${property} Asc")
				.get("/"+entitySetName+"?$orderby=${property} asc").check(status.is(200)))
			.pause(1)
			.exec(http("OrderBy "+entitySetName+" ${property} Desc")
				.get("/"+entitySetName+"?$orderby=${property} desc").check(status.is(200)))
			.pause(1)
		}
	}

	// fields in properties feeders: property, eqValue, neValue
	object Connections extends 			EntitySet("Connections", 		csv("filterable-Connections-properties.csv").circular, 9)
	object Networks extends 			EntitySet("Networks", 			csv("filterable-Networks-properties.csv").circular, 1)
	object Products extends 			EntitySet("Products", 			csv("filterable-Products-properties.csv").circular, 8)
	object Synchronizers extends 		EntitySet("Synchronizers", 		csv("filterable-Synchronizers-properties.csv").circular, 16)
	object Users extends 				EntitySet("Users", 				csv("filterable-Users-properties.csv").circular, 12)
	object Ingests extends 				EntitySet("Ingests", 			csv("filterable-Ingests-properties.csv").circular, 6)
	object UserSynchronizers extends 	EntitySet("UserSynchronizers", 	csv("filterable-UserSynchronizers-properties.csv").circular, 15)
	object Collections extends 			EntitySet("Collections", 		csv("filterable-Collections-properties.csv").circular, 2)
	object Classes extends 				EntitySet("Classes", 			csv("filterable-Classes-properties.csv").circular, 2)
	object DeletedProducts extends 		EntitySet("DeletedProducts", 	csv("filterable-DeletedProducts-properties.csv").circular, 10)
	object Scanners extends 			EntitySet("Scanners", 			csv("filterable-Scanners-properties.csv").circular, 7)

	val filterConnections = 		scenario("Connections").exec(Connections.filter)
	val filterNetworks = 			scenario("Networks").exec(Networks.filter)
	val filterProducts = 			scenario("Products").exec(Products.filter)
	val filterSynchronizers = 		scenario("Synchronizers").exec(Synchronizers.filter)
	val filterUsers = 				scenario("Users").exec(Users.filter)
	val filterIngests = 			scenario("Ingests").exec(Ingests.filter)
	val filterUserSynchronizers = 	scenario("UserSynchronizers").exec(UserSynchronizers.filter)
	val filterCollections = 		scenario("Collections").exec(Collections.filter)
	val filterClasses = 			scenario("Classes").exec(Classes.filter)
	val filterDeletedProducts = 	scenario("DeletedProducts").exec(DeletedProducts.filter)
	val filterScanners = 			scenario("Scanners").exec(Scanners.filter)

	setUp(
    	filterConnections.inject(		nothingFor( 0 seconds), rampUsers(1) over (20 seconds)), // scenario will be executed i times over n seconds
    	filterNetworks.inject(			nothingFor( 5 seconds), rampUsers(1) over (20 seconds)),
    	filterProducts.inject(			nothingFor(10 seconds), rampUsers(1) over (20 seconds)),
    	filterSynchronizers.inject(		nothingFor(15 seconds), rampUsers(1) over (20 seconds)),
    	filterUsers.inject(				nothingFor(20 seconds), rampUsers(1) over (20 seconds)),
    	filterIngests.inject(			nothingFor(25 seconds), rampUsers(1) over (20 seconds)),
    	filterUserSynchronizers.inject(	nothingFor(30 seconds), rampUsers(1) over (20 seconds)),
    	filterCollections.inject(		nothingFor(35 seconds), rampUsers(1) over (20 seconds)),
    	filterClasses.inject(			nothingFor(40 seconds), rampUsers(1) over (20 seconds)),
    	filterDeletedProducts.inject(	nothingFor(45 seconds), rampUsers(1) over (20 seconds)),
    	filterScanners.inject(	nothingFor(50 seconds), rampUsers(1) over (20 seconds)))
	.assertions(global.failedRequests.percent.is(0))
  	.protocols(httpConf)
}