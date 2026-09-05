-- ====================================================================
-- SafeRoads: Big Data Analytics Framework using Apache Hive
-- File: 02_analytical_queries.hql
-- Description: 12+ Comprehensive Production Analytical Queries
--              Covering Window Functions, Rollups, CTEs, and Binning.
-- ====================================================================

USE saferoads_db;

-- --------------------------------------------------------------------
-- QUERY 1: Severity Level Distribution & Average Delay Duration
-- Purpose: Understand the macroeconomic spread of crash severities and typical delays.
-- --------------------------------------------------------------------
SELECT 
    severity,
    COUNT(*) AS total_accidents,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) AS percentage_of_total,
    ROUND(AVG(duration_minutes), 2) AS mean_duration_minutes,
    ROUND(AVG(distance_mi), 3) AS mean_road_closure_miles
FROM us_accidents
GROUP BY severity
ORDER BY severity ASC;


-- --------------------------------------------------------------------
-- QUERY 2: Top 3 Most Hazardous Counties per State (Window Function: DENSE_RANK)
-- Purpose: Pinpoint high-risk counties across all states for targeted emergency response.
-- --------------------------------------------------------------------
WITH county_rankings AS (
    SELECT 
        state,
        county,
        COUNT(*) AS total_crashes,
        ROUND(AVG(severity), 2) AS avg_severity,
        DENSE_RANK() OVER (PARTITION BY state ORDER BY COUNT(*) DESC) AS risk_rank
    FROM us_accidents
    WHERE county IS NOT NULL AND county != 'Unknown'
    GROUP BY state, county
)
SELECT 
    state,
    risk_rank,
    county,
    total_crashes,
    avg_severity
FROM county_rankings
WHERE risk_rank <= 3
ORDER BY state ASC, risk_rank ASC;


-- --------------------------------------------------------------------
-- QUERY 3: Rush-Hour Temporal Crash Density Analysis
-- Purpose: Detect peak accident hours and examine severe crash concentration.
-- --------------------------------------------------------------------
SELECT 
    accident_hour,
    COUNT(*) AS total_incidents,
    SUM(CASE WHEN severity >= 3 THEN 1 ELSE 0 END) AS high_severity_incidents,
    ROUND(SUM(CASE WHEN severity >= 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS high_severity_rate_pct,
    ROUND(AVG(distance_mi), 3) AS avg_congestion_tail_miles
FROM us_accidents
GROUP BY accident_hour
ORDER BY total_incidents DESC;


-- --------------------------------------------------------------------
-- QUERY 4: Adverse Weather Condition Impact on Crash Severity & Duration
-- Purpose: Evaluate how adverse weather degrades road safety and inflates clearance time.
-- --------------------------------------------------------------------
SELECT 
    weather_condition,
    COUNT(*) AS crash_volume,
    ROUND(AVG(severity), 3) AS mean_severity_score,
    ROUND(AVG(duration_minutes), 1) AS mean_clearance_minutes
FROM us_accidents
WHERE weather_condition IS NOT NULL AND weather_condition != 'Unknown'
GROUP BY weather_condition
HAVING COUNT(*) >= 5000
ORDER BY mean_severity_score DESC
LIMIT 15;


-- --------------------------------------------------------------------
-- QUERY 5: Diurnal Risk Contrast: Day vs. Night Fatality & Crash Discrepancy
-- Purpose: Compare nighttime vs daytime crash frequencies and average severity per state.
-- --------------------------------------------------------------------
SELECT 
    state,
    COUNT(*) AS total_state_accidents,
    SUM(CASE WHEN sunrise_sunset = 'Night' THEN 1 ELSE 0 END) AS night_accidents,
    SUM(CASE WHEN sunrise_sunset = 'Day' THEN 1 ELSE 0 END) AS day_accidents,
    ROUND(AVG(CASE WHEN sunrise_sunset = 'Night' THEN severity ELSE NULL END), 2) AS night_avg_severity,
    ROUND(AVG(CASE WHEN sunrise_sunset = 'Day' THEN severity ELSE NULL END), 2) AS day_avg_severity
FROM us_accidents
GROUP BY state
ORDER BY total_state_accidents DESC
LIMIT 12;


-- --------------------------------------------------------------------
-- QUERY 6: Road Infrastructure Hazard Benchmark (Junction vs. Signal vs. Crossing)
-- Purpose: Identify which physical road feature correlates with higher severity.
-- --------------------------------------------------------------------
SELECT 
    'Highway Junction' AS infrastructure_category,
    COUNT(*) AS incident_count,
    ROUND(AVG(severity), 2) AS avg_severity,
    ROUND(AVG(duration_minutes), 1) AS avg_delay_minutes
FROM us_accidents WHERE junction_flag = 1
UNION ALL
SELECT 
    'Traffic Signal' AS infrastructure_category,
    COUNT(*) AS incident_count,
    ROUND(AVG(severity), 2) AS avg_severity,
    ROUND(AVG(duration_minutes), 1) AS avg_delay_minutes
FROM us_accidents WHERE traffic_signal_flag = 1
UNION ALL
SELECT 
    'Pedestrian Crossing' AS infrastructure_category,
    COUNT(*) AS incident_count,
    ROUND(AVG(severity), 2) AS avg_severity,
    ROUND(AVG(duration_minutes), 1) AS avg_delay_minutes
FROM us_accidents WHERE crossing_flag = 1
UNION ALL
SELECT 
    'Commercial Amenity Vicinity' AS infrastructure_category,
    COUNT(*) AS incident_count,
    ROUND(AVG(severity), 2) AS avg_severity,
    ROUND(AVG(duration_minutes), 1) AS avg_delay_minutes
FROM us_accidents WHERE amenity_flag = 1;


-- --------------------------------------------------------------------
-- QUERY 7: Year-over-Year (YoY) Crash Volume Trajectory (Window Function: LAG)
-- Purpose: Track long-term crash volume trends and growth rates across recorded years.
-- --------------------------------------------------------------------
WITH yearly_aggregates AS (
    SELECT 
        accident_year,
        COUNT(*) AS annual_crashes
    FROM us_accidents
    WHERE accident_year BETWEEN 2016 AND 2023
    GROUP BY accident_year
)
SELECT 
    accident_year,
    annual_crashes,
    LAG(annual_crashes, 1) OVER (ORDER BY accident_year) AS prior_year_crashes,
    ROUND(((annual_crashes - LAG(annual_crashes, 1) OVER (ORDER BY accident_year)) * 100.0) /
          LAG(annual_crashes, 1) OVER (ORDER BY accident_year), 2) AS yoy_growth_rate_pct
FROM yearly_aggregates
ORDER BY accident_year ASC;


-- --------------------------------------------------------------------
-- QUERY 8: Atmospheric Visibility Impairment Categorization & Crash Severity
-- Purpose: Analyze safety metrics across four explicit visibility brackets.
-- --------------------------------------------------------------------
SELECT 
    CASE 
        WHEN visibility_mi < 1.0 THEN 'Tier 1: Extreme Low Fog/Smoke (<1 mi)'
        WHEN visibility_mi >= 1.0 AND visibility_mi < 3.0 THEN 'Tier 2: Restricted Visibility (1-3 mi)'
        WHEN visibility_mi >= 3.0 AND visibility_mi < 7.0 THEN 'Tier 3: Moderate Visibility (3-7 mi)'
        ELSE 'Tier 4: Optimal Visibility (>7 mi)'
    END AS visibility_risk_bracket,
    COUNT(*) AS crash_occurrences,
    ROUND(AVG(severity), 2) AS average_severity,
    ROUND(AVG(distance_mi), 3) AS avg_blocked_lane_miles
FROM us_accidents
WHERE visibility_mi IS NOT NULL
GROUP BY 
    CASE 
        WHEN visibility_mi < 1.0 THEN 'Tier 1: Extreme Low Fog/Smoke (<1 mi)'
        WHEN visibility_mi >= 1.0 AND visibility_mi < 3.0 THEN 'Tier 2: Restricted Visibility (1-3 mi)'
        WHEN visibility_mi >= 3.0 AND visibility_mi < 7.0 THEN 'Tier 3: Moderate Visibility (3-7 mi)'
        ELSE 'Tier 4: Optimal Visibility (>7 mi)'
    END
ORDER BY average_severity DESC;


-- --------------------------------------------------------------------
-- QUERY 9: State-Level Severity Aggregation using Hierarchical ROLLUP
-- Purpose: Generate subtotal and grand-total accident metrics across the federation.
-- --------------------------------------------------------------------
SELECT 
    COALESCE(state, 'ALL_STATES_TOTAL') AS state_id,
    COUNT(*) AS total_accidents,
    SUM(CASE WHEN severity >= 3 THEN 1 ELSE 0 END) AS total_severe_accidents,
    ROUND(SUM(CASE WHEN severity >= 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS severe_accident_pct
FROM us_accidents
GROUP BY state
WITH ROLLUP;


-- --------------------------------------------------------------------
-- QUERY 10: Precipitation Volume vs Traffic Queuing Distance
-- Purpose: Determine rainfall thresholds causing the longest traffic bottlenecks.
-- --------------------------------------------------------------------
SELECT 
    CASE 
        WHEN precipitation_in = 0.0 THEN '0. Zero Precipitation'
        WHEN precipitation_in > 0.0 AND precipitation_in <= 0.10 THEN '1. Drizzle (0.01 - 0.10 in)'
        WHEN precipitation_in > 0.10 AND precipitation_in <= 0.50 THEN '2. Steady Rain (0.11 - 0.50 in)'
        ELSE '3. Torrential Downpour (>0.50 in)'
    END AS precipitation_category,
    COUNT(*) AS accident_count,
    ROUND(AVG(distance_mi), 3) AS avg_impact_distance_miles,
    ROUND(AVG(duration_minutes), 1) AS avg_clearance_minutes
FROM us_accidents
WHERE precipitation_in IS NOT NULL
GROUP BY 
    CASE 
        WHEN precipitation_in = 0.0 THEN '0. Zero Precipitation'
        WHEN precipitation_in > 0.0 AND precipitation_in <= 0.10 THEN '1. Drizzle (0.01 - 0.10 in)'
        WHEN precipitation_in > 0.10 AND precipitation_in <= 0.50 THEN '2. Steady Rain (0.11 - 0.50 in)'
        ELSE '3. Torrential Downpour (>0.50 in)'
    END
ORDER BY precipitation_category ASC;


-- --------------------------------------------------------------------
-- QUERY 11: Top 5 Outlier Prolonged Road Closure Events per State (ROW_NUMBER)
-- Purpose: Identify catastrophic multi-hour highway gridlocks.
-- --------------------------------------------------------------------
WITH state_closure_rankings AS (
    SELECT 
        id,
        state,
        city,
        county,
        severity,
        weather_condition,
        duration_minutes,
        ROW_NUMBER() OVER (PARTITION BY state ORDER BY duration_minutes DESC) AS duration_rank
    FROM us_accidents
    WHERE duration_minutes > 0
)
SELECT 
    state,
    duration_rank,
    id,
    city,
    county,
    severity,
    weather_condition,
    duration_minutes
FROM state_closure_rankings
WHERE duration_rank <= 5
ORDER BY state ASC, duration_rank ASC;


-- --------------------------------------------------------------------
-- QUERY 12: Composite High-Risk Urban Hotspot Indexing (CTE & Weighted Scoring)
-- Purpose: Synthesizes accident volume, severity, and intersection complexity into a single risk index.
-- --------------------------------------------------------------------
WITH urban_metrics AS (
    SELECT 
        state,
        city,
        COUNT(*) AS total_accidents,
        ROUND(AVG(severity), 3) AS avg_severity,
        SUM(junction_flag) AS junction_crashes,
        SUM(traffic_signal_flag) AS signal_crashes
    FROM us_accidents
    WHERE city IS NOT NULL AND city != 'Unknown'
    GROUP BY state, city
    HAVING COUNT(*) >= 1500
)
SELECT 
    state,
    city,
    total_accidents,
    avg_severity,
    junction_crashes,
    signal_crashes,
    ROUND(
        (avg_severity * 0.40) + 
        ((total_accidents / 1000.0) * 0.35) + 
        (((junction_crashes + signal_crashes) * 1.0 / total_accidents) * 0.25), 
        3
    ) AS composite_danger_index
FROM urban_metrics
ORDER BY composite_danger_index DESC
LIMIT 20;
