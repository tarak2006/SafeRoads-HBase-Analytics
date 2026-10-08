#!/usr/bin/env python3
"""
SafeRoads: Batch Generator / Ingestor for Apache HBase
Reads the real-world US Accidents dataset (accidents_cleaned.tsv) and generates
optimized HBase PUT commands for HBase Shell or Java API ingestion.
"""

import sys
import os

def generate_puts(tsv_path, output_rb_path, limit=50):
    print(f"Reading dataset from: {tsv_path}")
    if not os.path.exists(tsv_path):
        print(f"Dataset not found at {tsv_path}")
        return

    with open(tsv_path, 'r', encoding='utf-8') as fin, open(output_rb_path, 'w', encoding='utf-8') as fout:
        header = fin.readline().strip().split('\t')
        fout.write("# Auto-generated HBase Shell PUT commands from SafeRoads Real-World Dataset\n")
        
        count = 0
        for line in fin:
            parts = line.strip().split('\t')
            if len(parts) < 21:
                continue

            aid = parts[0]
            severity = parts[1]
            start_time = parts[2]
            end_time = parts[3]
            lat = parts[4]
            lng = parts[5]
            dist = parts[6]
            city = parts[7].replace("'", "\\'")
            county = parts[8].replace("'", "\\'")
            state = parts[9]
            temp = parts[11] if len(parts) > 11 else ""
            humidity = parts[12] if len(parts) > 12 else ""
            visibility = parts[13] if len(parts) > 13 else ""
            weather = parts[16].replace("'", "\\'") if len(parts) > 16 else ""
            amenity = parts[17] if len(parts) > 17 else "0"
            crossing = parts[18] if len(parts) > 18 else "0"
            junction = parts[19] if len(parts) > 19 else "0"
            traffic_signal = parts[20] if len(parts) > 20 else "0"
            day_night = parts[21] if len(parts) > 21 else "Day"

            date_str = start_time.split(' ')[0] if ' ' in start_time else start_time
            row_key = f"{state}#{severity}#{date_str}#{aid}"

            # Loc family
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'loc:state', '{state}'\n")
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'loc:city', '{city}'\n")
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'loc:lat', '{lat}'\n")
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'loc:lng', '{lng}'\n")
            # Time family
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'time:start_time', '{start_time}'\n")
            # Env family
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'env:weather', '{weather}'\n")
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'env:temp_f', '{temp}'\n")
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'env:day_night', '{day_night}'\n")
            # Hazard family
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'hazard:severity', '{severity}'\n")
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'hazard:junction', '{junction}'\n")
            fout.write(f"put 'saferoads_accidents', '{row_key}', 'hazard:traffic_signal', '{traffic_signal}'\n")

            count += 1
            if count >= limit:
                break

        print(f"Generated {count} records into {output_rb_path}")

if __name__ == '__main__':
    tsv = sys.argv[1] if len(sys.argv) > 1 else 'data/processed/accidents_cleaned.tsv'
    out = sys.argv[2] if len(sys.argv) > 2 else 'hbase/scripts/02_batch_records.hbase'
    lim = int(sys.argv[3]) if len(sys.argv) > 3 else 50
    generate_puts(tsv, out, lim)
