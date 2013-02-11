package org.dhus.metrics.external.influx.database;

import org.influxdb.annotation.Measurement;

@Measurement(name="daily_timers_by_source")
public class ProdSyncDailyTimersBySourcePOJO extends ProdSyncTimerBySourcePOJO
{

}
