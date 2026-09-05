# SafeRoads: Hive Query Execution Results

### Query 1: Severity Distribution & Average Delay

| severity | total_accidents | pct_of_total | mean_duration_mins | mean_distance_miles |
| --- | --- | --- | --- | --- |
| 1 | 48 | 0.1 | 39.0 | 0.003 |
| 2 | 27415 | 54.83 | 42.61 | 0.013 |
| 3 | 22511 | 45.02 | 42.53 | 0.01 |
| 4 | 25 | 0.05 | 43.76 | 0.006 |

---
### Query 2: Top 3 Hazardous Counties per State (Window Function: DENSE_RANK)

| state | rank | county | crash_count | avg_severity |
| --- | --- | --- | --- | --- |
| CA | 1 | Los Angeles | 16761 | 2.52 |
| CA | 2 | Alameda | 3772 | 2.78 |
| CA | 3 | San Diego | 3760 | 2.55 |
| OH | 1 | Montgomery | 174 | 2.15 |
| OH | 2 | Franklin | 110 | 2.55 |
| OH | 3 | Cuyahoga | 19 | 2.74 |
| WV | 1 | Putnam | 1 | 2.0 |

---
### Query 3: Rush-Hour Crash Density Analysis

| accident_hour | hourly_crashes | high_severity_crashes | high_severity_rate_pct | avg_queue_length_miles |
| --- | --- | --- | --- | --- |
| 11 | 3984 | 1748 | 43.88 | 0.011 |
| 20 | 3695 | 1674 | 45.3 | 0.009 |
| 10 | 3631 | 1627 | 44.81 | 0.011 |
| 19 | 3366 | 1496 | 44.44 | 0.007 |
| 12 | 3101 | 1404 | 45.28 | 0.017 |
| 18 | 3079 | 1354 | 43.98 | 0.016 |
| 9 | 2657 | 1275 | 47.99 | 0.016 |
| 16 | 2652 | 1122 | 42.31 | 0.008 |
| 14 | 2593 | 1153 | 44.47 | 0.014 |
| 13 | 2560 | 1129 | 44.1 | 0.01 |

---
### Query 4: Weather Impact on Crash Severity & Duration

| weather_condition | crash_volume | mean_severity | mean_duration_mins |
| --- | --- | --- | --- |
| Heavy Rain | 110 | 2.6 | 38.9 |
| Rain | 407 | 2.504 | 38.6 |
| Partly Cloudy | 4161 | 2.496 | 41.9 |
| Haze | 1046 | 2.494 | 41.0 |
| Mostly Cloudy | 4386 | 2.488 | 40.6 |
| Light Rain | 1604 | 2.485 | 39.6 |
| Scattered Clouds | 2816 | 2.484 | 41.6 |
| Clear | 29395 | 2.436 | 43.6 |
| Overcast | 5177 | 2.434 | 41.1 |
| Light Snow | 58 | 2.431 | 48.1 |

---
### Query 5: Diurnal Risk Contrast (Day vs. Night Crash Discrepancy)

| state | total_crashes | night_crashes | day_crashes | night_avg_severity | day_avg_severity |
| --- | --- | --- | --- | --- | --- |
| CA | 49635 | 18665 | 30970 | 2.46 | 2.45 |
| OH | 363 | 102 | 261 | 2.42 | 2.31 |
| WV | 1 | 0 | 1 |  | 2.0 |

---
### Query 6: Road Infrastructure Benchmark (Junction vs. Signal vs. Crossing)

| infrastructure | incident_count | avg_severity | avg_delay_mins |
| --- | --- | --- | --- |
| Highway Junction | 5201 | 2.6 | 41.5 |
| Traffic Signal | 5385 | 2.17 | 42.9 |
| Pedestrian Crossing | 3212 | 2.15 | 44.2 |
| Commercial Amenity | 387 | 2.08 | 41.3 |

---
### Query 7: Year-over-Year (YoY) Crash Trajectory (Window Function: LAG)

| accident_year | annual_crashes | prev_year_crashes | yoy_pct_growth |
| --- | --- | --- | --- |
| 2016 | 45329 |  |  |
| 2017 | 4670 | 45329 | -89.7 |

---
### Query 8: Visibility Risk Brackets

| visibility_bracket | crash_count | avg_severity | avg_blocked_miles |
| --- | --- | --- | --- |
| Tier 3: Moderate (3-7 mi) | 2774 | 2.46 | 0.016 |
| Tier 4: Normal (>7 mi) | 46074 | 2.45 | 0.011 |
| Tier 2: Restricted (1-3 mi) | 949 | 2.45 | 0.015 |
| Tier 1: Extreme Low (<1 mi) | 202 | 2.33 | 0.015 |

---
### Query 9: Top 5 Outlier Road Blockages (Window Function: ROW_NUMBER)

| state | rn | id | city | severity | weather_condition | duration_minutes |
| --- | --- | --- | --- | --- | --- | --- |
| CA | 1 | A-42598 | Sonoma | 2 | Clear | 503.5 |
| CA | 2 | A-42712 | Lafayette | 2 | Clear | 471.5 |
| CA | 3 | A-42716 | Muir Beach | 2 | Partly Cloudy | 468.5 |
| OH | 1 | A-92 | Cleveland | 3 | Overcast | 871.0 |
| OH | 2 | A-136 | Columbus | 3 | Scattered Clouds | 826.0 |
| OH | 3 | A-222 | Barberton | 3 | Haze | 791.0 |
| WV | 1 | A-364 | Fraziers Bottom | 2 | Cloudy | 45.0 |

---
### Query 10: Composite High-Risk Urban Hotspot Index (CTE Query)

| state | city | total_accidents | avg_severity | junction_crashes | signal_crashes | composite_danger_index |
| --- | --- | --- | --- | --- | --- | --- |
| CA | Los Angeles | 4908 | 2.524 | 540 | 798 | 18.256 |
| CA | Sacramento | 2540 | 2.331 | 279 | 306 | 9.88 |
| CA | San Diego | 1599 | 2.679 | 166 | 66 | 6.704 |
| CA | San Jose | 1493 | 2.436 | 210 | 78 | 6.248 |
| CA | Oakland | 924 | 2.788 | 237 | 109 | 4.443 |
| CA | Long Beach | 668 | 2.74 | 96 | 59 | 3.492 |
| CA | San Francisco | 648 | 2.559 | 125 | 91 | 3.375 |
| CA | Riverside | 609 | 2.43 | 42 | 32 | 3.134 |
| CA | Corona | 554 | 2.253 | 19 | 17 | 2.856 |
| CA | Whittier | 503 | 2.501 | 26 | 113 | 2.83 |

---
