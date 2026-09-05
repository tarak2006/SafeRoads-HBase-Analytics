-- ====================================================================
-- SafeRoads: Big Data Analytics Framework using Apache Hive
-- File: 01_create_tables.hql
-- Description: DDL scripts for raw external table, staging, and optimized
--              partitioned/bucketed Hive tables for high-speed queries.
-- ====================================================================

-- 1. Create Database
CREATE DATABASE IF NOT EXISTS saferoads_db;
USE saferoads_db;

-- 2. Staging External Table mapping directly to preprocessed TSV file
-- Storage path in HDFS or local file system
DROP TABLE IF EXISTS us_accidents_raw;

CREATE EXTERNAL TABLE IF NOT EXISTS us_accidents_raw (
    id STRING,
    severity INT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    start_lat DOUBLE,
    start_lng DOUBLE,
    distance_mi DOUBLE,
    city STRING,
    county STRING,
    state STRING,
    timezone STRING,
    temperature_f DOUBLE,
    humidity_pct DOUBLE,
    visibility_mi DOUBLE,
    wind_speed_mph DOUBLE,
    precipitation_in DOUBLE,
    weather_condition STRING,
    amenity_flag INT,
    crossing_flag INT,
    junction_flag INT,
    traffic_signal_flag INT,
    sunrise_sunset STRING,
    accident_year INT,
    accident_month INT,
    accident_hour INT,
    accident_day_of_week STRING,
    duration_minutes DOUBLE
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/saferoads_db.db/raw_accidents'
TBLPROPERTIES ("skip.header.line.count"="1");

-- Note for Local/CLI load:
-- LOAD DATA LOCAL INPATH 'data/processed/accidents_cleaned.tsv' OVERWRITE INTO TABLE us_accidents_raw;


-- 3. Optimized Production Table: Partitioned by State & Bucketed by City into ORC format
-- (Provides 10x-50x faster analytical query execution in Big Data environments)
DROP TABLE IF EXISTS us_accidents;

CREATE TABLE IF NOT EXISTS us_accidents (
    id STRING,
    severity INT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    start_lat DOUBLE,
    start_lng DOUBLE,
    distance_mi DOUBLE,
    city STRING,
    county STRING,
    timezone STRING,
    temperature_f DOUBLE,
    humidity_pct DOUBLE,
    visibility_mi DOUBLE,
    wind_speed_mph DOUBLE,
    precipitation_in DOUBLE,
    weather_condition STRING,
    amenity_flag INT,
    crossing_flag INT,
    junction_flag INT,
    traffic_signal_flag INT,
    sunrise_sunset STRING,
    accident_year INT,
    accident_month INT,
    accident_hour INT,
    accident_day_of_week STRING,
    duration_minutes DOUBLE
)
PARTITIONED BY (state STRING)
CLUSTERED BY (city) INTO 16 BUCKETS
STORED AS ORC
TBLPROPERTIES ("orc.compress"="SNAPPY");

-- 4. Dynamic Partitioning Ingestion from Staging into Partitioned Table
SET hive.exec.dynamic.partition = true;
SET hive.exec.dynamic.partition.mode = nonstrict;
SET hive.enforce.bucketing = true;

-- Query to populate partitioned table:
-- INSERT OVERWRITE TABLE us_accidents PARTITION(state)
-- SELECT 
--     id, severity, start_time, end_time, start_lat, start_lng, distance_mi,
--     city, county, timezone, temperature_f, humidity_pct, visibility_mi,
--     wind_speed_mph, precipitation_in, weather_condition, amenity_flag,
--     crossing_flag, junction_flag, traffic_signal_flag, sunrise_sunset,
--     accident_year, accident_month, accident_hour, accident_day_of_week,
--     duration_minutes, state
-- FROM us_accidents_raw;
