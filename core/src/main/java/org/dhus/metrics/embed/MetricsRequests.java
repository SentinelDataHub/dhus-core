package org.dhus.metrics.embed;

public class MetricsRequests
{

   public static final String SELECT_COUNT = "SELECT COUNT(1) FROM metrics";
   
   public static final String GROUP_BY_CLAUSE =  " group by name, type";
   
   //We can add more condition in the request
   // Do not forget to add @GROUP_BY_CLAUSE
   public static final String HOURLY_COUNTER_REQ = "select max(count) as count, name, type from metrics "
         + " where name like 'prod_sync%counters%' "
         + " and date >= now() - '1' HOUR " ;
   
   public static final String DAILY_COUNTER_REQ = "select max(count) as count, name, type from metrics "
         + " where name like 'prod_sync%counters%' "
         + " and date >= now() - '1' DAY " ;
   
   public static final String MONTHLY_COUNTER_REQ = "select max(count) as count, name, type from metrics "
         + " where name like 'prod_sync%counters%' "
         + " and date >= now() - '30' DAY " ;
   
   public static final String COUNT_REQ = "select count(1) from (" ; // + HOURLY_COUNTER_REQ + ")";
   public static final String END_COUNT_REQ = ")";
   
   //public static String COUNT_DAILY_COUNTER_REQ = "select count(1) from (" + DAILY_COUNTER_REQ + ")";
   
//   public static String COUNT_MONTHLY_COUNTER_REQ = "select count(1) from (" + MONTHLY_COUNTER_REQ + ")";
   
   public static final String HOURLY_TIMER_REQ = "select name, type, avg(max) as max, avg(min) as min, avg(mean) as mean, avg(median) as median, avg(std_dev) as std_dev, avg(h_75thpercentile) as h_75thpercentile, " + 
         "avg(h_95thpercentile) as h_95thpercentile, avg(h_98thpercentile) as h_98thpercentile, avg(h_99thpercentile) as h_99thpercentile, avg(h_999thpercentile) as h_999thpercentile, avg(mean_rate) as mean_rate, " + 
         "avg(m_1m_rate) as m_1m_rate, avg(m_5m_rate) as m_5m_rate,avg(m_15m_rate) as m_15m_rate, max(count) as count  from metrics " + 
         " where name like 'prod_sync%timers%' and date >= now() - '1' HOUR " ;
   
   public static final String DAILY_TIMER_REQ = "select name, type, avg(max) as max, avg(min) as min, avg(mean) as mean, avg(median) as median, avg(std_dev) as std_dev, avg(h_75thpercentile) as h_75thpercentile, " + 
         "avg(h_95thpercentile) as h_95thpercentile, avg(h_98thpercentile) as h_98thpercentile, avg(h_99thpercentile) as h_99thpercentile, avg(h_999thpercentile) as h_999thpercentile, avg(mean_rate) as mean_rate, " + 
         "avg(m_1m_rate) as m_1m_rate, avg(m_5m_rate) as m_5m_rate,avg(m_15m_rate) as m_15m_rate, max(count) as count  from metrics " + 
         " where name like 'prod_sync%timers%' and date >= now() - '1' DAY " ;
   
   public static final String MONTHLY_TIMER_REQ = "select name, type, avg(max) as max, avg(min) as min, avg(mean) as mean, avg(median) as median, avg(std_dev) as std_dev, avg(h_75thpercentile) as h_75thpercentile, " + 
         "avg(h_95thpercentile) as h_95thpercentile, avg(h_98thpercentile) as h_98thpercentile, avg(h_99thpercentile) as h_99thpercentile, avg(h_999thpercentile) as h_999thpercentile, avg(mean_rate) as mean_rate, " + 
         "avg(m_1m_rate) as m_1m_rate, avg(m_5m_rate) as m_5m_rate,avg(m_15m_rate) as m_15m_rate, max(count) as count  from metrics " + 
         " where name like 'prod_sync%timers%' and date >= now() - '30' DAY " ;
   
//   public static String COUNT_HOURLY_TIMER_REQ = "select count(1) from (" + HOURLY_TIMER_REQ + ")";
   
//   public static String COUNT_DAILY_TIMER_REQ = "select count(1) from (" + DAILY_TIMER_REQ + ")";
   
//   public static String COUNT_MONTHLY_TIMER_REQ = "select count(1) from (" + MONTHLY_TIMER_REQ + ")";
   
}
