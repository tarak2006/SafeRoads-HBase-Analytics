#!/usr/bin/env python3
"""
preprocess.py
End-to-End Preprocessing and ETL Pipeline for SafeRoads Big Data Analytics.
Streams the raw Kaggle US Accidents CSV (1.42 GB) with low memory footprint,
performs cleaning, null imputation, timestamp feature engineering,
and writes an optimized Tab-Separated (TSV) dataset for Hadoop MapReduce & Hive.
"""

import sys
import os
import glob
import csv
from datetime import datetime

def find_input_file():
    """Locates the raw Kaggle CSV if present, otherwise falls back to sample CSV."""
    raw_files = glob.glob("data/raw/*.csv")
    if raw_files:
        return raw_files[0]
    root_csvs = glob.glob("data/*.csv")
    if root_csvs:
        return root_csvs[0]
    sample_file = "data/sample/us_accidents_sample.csv"
    if os.path.exists(sample_file):
        return sample_file
    return None

def parse_iso_datetime(dt_str):
    """Robust parser for different timestamp formats in US Accidents dataset."""
    if not dt_str:
        return None
    dt_str = dt_str.strip()
    formats = [
        "%Y-%m-%d %H:%M:%S",
        "%Y-%m-%d %H:%M:%S.%f",
        "%Y-%m-%dT%H:%M:%S",
        "%Y-%m-%dT%H:%M:%SZ"
    ]
    for fmt in formats:
        try:
            return datetime.strptime(dt_str, fmt)
        except ValueError:
            continue
    try:
        clean_str = dt_str.split(".")[0].replace("T", " ")
        return datetime.strptime(clean_str, "%Y-%m-%d %H:%M:%S")
    except Exception:
        return None

def clean_boolean(val):
    """Converts boolean strings to integer 1 or 0."""
    if not val:
        return 0
    val_lower = str(val).strip().lower()
    return 1 if val_lower in ("true", "t", "1", "yes") else 0

def clean_float(val, default=0.0):
    """Safely casts string to float with fallback."""
    if not val:
        return default
    try:
        return round(float(val), 2)
    except (ValueError, TypeError):
        return default

def clean_int(val, default=0):
    """Safely casts string to integer with fallback."""
    if not val:
        return default
    try:
        return int(float(val))
    except (ValueError, TypeError):
        return default

def run_preprocessing(input_path=None, output_path="data/processed/accidents_cleaned.tsv"):
    if input_path is None:
        input_path = find_input_file()
    
    if not input_path or not os.path.exists(input_path):
        print(f"[ERROR] No input file found. Looked in data/raw/, data/, and data/sample/.")
        sys.exit(1)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    print(f"============================================================")
    print(f"[START] Preprocessing Pipeline")
    print(f"  Input File  : {input_path} ({os.path.getsize(input_path) / (1024*1024):.2f} MB)")
    print(f"  Output File : {output_path}")
    print(f"============================================================")

    total_rows = 0
    valid_rows = 0
    skipped_rows = 0

    output_headers = [
        "id", "severity", "start_time", "end_time", "start_lat", "start_lng",
        "distance_mi", "city", "county", "state", "timezone",
        "temperature_f", "humidity_pct", "visibility_mi", "wind_speed_mph",
        "precipitation_in", "weather_condition", "amenity_flag", "crossing_flag",
        "junction_flag", "traffic_signal_flag", "sunrise_sunset",
        "accident_year", "accident_month", "accident_hour", "accident_day_of_week",
        "duration_minutes"
    ]

    with open(input_path, mode="r", encoding="utf-8", errors="replace") as fin, \
         open(output_path, mode="w", encoding="utf-8", newline="") as fout:

        reader = csv.reader(fin)
        writer = csv.writer(fout, delimiter="\t", lineterminator="\n")

        header = next(reader, None)
        if not header:
            print("[ERROR] Input file is completely empty.")
            sys.exit(1)

        # Build column index map to support any minor column index shifts
        col_map = {col.strip().lower(): idx for idx, col in enumerate(header)}

        def get_val(row, col_name, default=""):
            idx = col_map.get(col_name.lower())
            if idx is not None and idx < len(row):
                return row[idx].strip()
            return default

        # Write clean TSV header
        writer.writerow(output_headers)

        for row in reader:
            total_rows += 1
            if total_rows % 500000 == 0:
                print(f"[PROGRESS] Processed {total_rows:,} rows... ({valid_rows:,} valid exported)")

            acc_id = get_val(row, "id")
            severity_str = get_val(row, "severity")
            start_time_str = get_val(row, "start_time")
            end_time_str = get_val(row, "end_time")
            state = get_val(row, "state").upper()

            # Crucial check: Drop if primary identifiers are missing
            if not acc_id or not start_time_str or not state or not severity_str:
                skipped_rows += 1
                continue

            # Parse timestamps & compute duration
            dt_start = parse_iso_datetime(start_time_str)
            dt_end = parse_iso_datetime(end_time_str) if end_time_str else None

            if not dt_start:
                skipped_rows += 1
                continue

            if dt_end and dt_end >= dt_start:
                duration_mins = round((dt_end - dt_start).total_seconds() / 60.0, 1)
            else:
                duration_mins = 60.0
                dt_end = dt_start

            # Prune impossible durations (> 30 days = 43200 mins or negative)
            if duration_mins < 0 or duration_mins > 43200:
                skipped_rows += 1
                continue

            severity = clean_int(severity_str, default=2)
            if severity < 1 or severity > 4:
                severity = 2

            start_lat = clean_float(get_val(row, "start_lat"), 0.0)
            start_lng = clean_float(get_val(row, "start_lng"), 0.0)
            distance_mi = clean_float(get_val(row, "distance(mi)"), 0.0)
            city = get_val(row, "city", "Unknown") or "Unknown"
            county = get_val(row, "county", "Unknown") or "Unknown"
            timezone = get_val(row, "timezone", "US/Eastern") or "US/Eastern"

            # Weather attributes
            temp_f = clean_float(get_val(row, "temperature(f)"), 65.0)
            humidity = clean_float(get_val(row, "humidity(%)"), 50.0)
            visibility = clean_float(get_val(row, "visibility(mi)"), 10.0)
            wind_speed = clean_float(get_val(row, "wind_speed(mph)"), 5.0)
            precip = clean_float(get_val(row, "precipitation(in)"), 0.0)

            weather_cond = get_val(row, "weather_condition", "Clear") or "Clear"
            weather_cond = weather_cond.replace("\t", " ").strip()

            # Infrastructure flags
            amenity_flag = clean_boolean(get_val(row, "amenity"))
            crossing_flag = clean_boolean(get_val(row, "crossing"))
            junction_flag = clean_boolean(get_val(row, "junction"))
            signal_flag = clean_boolean(get_val(row, "traffic_signal"))

            day_night = get_val(row, "sunrise_sunset", "Day")
            if day_night.lower() not in ("day", "night"):
                day_night = "Day" if 6 <= dt_start.hour <= 19 else "Night"

            # Feature engineered date fields
            accident_year = dt_start.year
            accident_month = dt_start.month
            accident_hour = dt_start.hour
            accident_day_of_week = dt_start.strftime("%A")

            cleaned_row = [
                acc_id,
                severity,
                dt_start.strftime("%Y-%m-%d %H:%M:%S"),
                dt_end.strftime("%Y-%m-%d %H:%M:%S"),
                start_lat,
                start_lng,
                distance_mi,
                city,
                county,
                state,
                timezone,
                temp_f,
                humidity,
                visibility,
                wind_speed,
                precip,
                weather_cond,
                amenity_flag,
                crossing_flag,
                junction_flag,
                signal_flag,
                day_night.capitalize(),
                accident_year,
                accident_month,
                accident_hour,
                accident_day_of_week,
                duration_mins
            ]

            writer.writerow(cleaned_row)
            valid_rows += 1

    out_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"============================================================")
    print(f"[COMPLETE] Preprocessing Finished!")
    print(f"  Total Processed : {total_rows:,} records")
    print(f"  Valid Exported  : {valid_rows:,} records ({valid_rows/max(1,total_rows)*100:.1f}%)")
    print(f"  Skipped/Pruned  : {skipped_rows:,} records")
    print(f"  Output TSV Size : {out_mb:.2f} MB ({out_mb/1024:.2f} GB) -> {output_path}")
    print(f"============================================================")

if __name__ == "__main__":
    in_file = sys.argv[1] if len(sys.argv) > 1 else None
    out_file = sys.argv[2] if len(sys.argv) > 2 else "data/processed/accidents_cleaned.tsv"
    run_preprocessing(in_file, out_file)
