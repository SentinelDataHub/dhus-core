package org.dhus.metrics.external.influx.database;

import org.influxdb.annotation.Measurement;

@Measurement(name="monthly_timers_by_source")
public class ProdSyncMonthlyTimersBySourcePOJO extends ProdSyncTimerBySourcePOJO
{

}
