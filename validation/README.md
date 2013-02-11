# DHuS Validation

Provides all the files necessary to setup and execute test and validation scenarios for the DHuS,
using [GoCD](https://www.gocd.io/) as an automated continuous integration pipeline, and [Gatling](http://gatling.io/)
as a framework for functional tests.

**Distribution**

Contains files required to generate a software distribution of the DHuS using Maven, including pom/assembly,
configuration files, and custom shell scripts. This distribution is destined to embark a newly built dhus-core
and withstand several test scenarios in order to validate it.

*Tied to DHuS releases (dependencies and configuration changes).*

**Gatling**

Contains files related to Gatling for functional tests aimed at validating the OData interface of the DHuS
by executing user requests and checking responses. Divided in two categories:

- data: CSV files containing reference data used in tests, commonly known as feeders.

- simulations: Test and validation scenarios in the form of Gatling simulations written in *Scala*.

*Tied to DHuS releases (OData interface changes).*

**Utility**

Contains several convenience programs and scripts to help prepare a test environment and monitor a
DHuS on startup.

*Tied to the execution environment of tests.*
