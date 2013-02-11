package org.dhus.metrics.external.influx.database;

import org.influxdb.annotation.Measurement;

@Measurement(name="hourly_timers_by_source")
public class ProdSyncHourlyTimersBySourcePOJO extends ProdSyncTimerBySourcePOJO
{

}
