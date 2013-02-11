package org.dhus.metrics.external.influx.database;


import org.influxdb.annotation.Measurement;

@Measurement(name="hourly_measures_by_source")
public class ProdSyncHourlyCounterBySourcePOJO extends ProdSyncCounterBySourcePOJO
{
   
}
