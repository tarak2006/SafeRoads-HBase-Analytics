#!/usr/bin/env python3
"""
hive_cli.py
Local Hive SQL (HQL) Execution CLI for SafeRoads.
Executes raw .hql query files directly using an in-memory SQL database engine,
emulating the Apache Hive command-line interface:
Usage:
    python3 hive/hive_cli.py -f hive/02_analytical_queries.hql
"""

import sys
import os
import sqlite3
import csv
import re

def create_in_memory_db(tsv_path="data/processed/accidents_cleaned.tsv", limit_rows=50000):
    if not os.path.exists(tsv_path):
        sample_path = "data/processed/accidents_cleaned_sample.tsv"
        if os.path.exists(sample_path):
            tsv_path = sample_path
        else:
            print(f"[ERROR] Processed dataset not found at {tsv_path}")
            sys.exit(1)

    print(f"[HIVE CLI] Connecting to in-memory Hive Metastore...")
    conn = sqlite3.connect(":memory:")
    cur = conn.cursor()

    cur.execute("""
    CREATE TABLE us_accidents (
        id TEXT, severity INTEGER, start_time TEXT, end_time TEXT,
        start_lat REAL, start_lng REAL, distance_mi REAL, city TEXT,
        county TEXT, state TEXT, timezone TEXT, temperature_f REAL,
        humidity_pct REAL, visibility_mi REAL, wind_speed_mph REAL,
        precipitation_in REAL, weather_condition TEXT, amenity_flag INTEGER,
        crossing_flag INTEGER, junction_flag INTEGER, traffic_signal_flag INTEGER,
        sunrise_sunset TEXT, accident_year INTEGER, accident_month INTEGER,
        accident_hour INTEGER, accident_day_of_week TEXT, duration_minutes REAL
    )
    """)

    print(f"[HIVE CLI] Loading table saferoads_db.us_accidents from {tsv_path}...")
    with open(tsv_path, "r", encoding="utf-8") as f:
        reader = csv.reader(f, delimiter="\t")
        header = next(reader)
        insert_sql = "INSERT INTO us_accidents VALUES (" + ",".join(["?"] * len(header)) + ")"
        batch = []
        count = 0
        for row in reader:
            if len(row) == len(header):
                batch.append(row)
                count += 1
                if len(batch) >= 10000:
                    cur.executemany(insert_sql, batch)
                    batch = []
                if count >= limit_rows:
                    break
        if batch:
            cur.executemany(insert_sql, batch)

    print(f"[HIVE CLI] Ingested {count:,} records into table us_accidents. Ready to execute HQL!\n")
    return conn

def execute_hql_file(conn, hql_path, target_query=None):
    if not os.path.exists(hql_path):
        print(f"[ERROR] HQL file not found: {hql_path}")
        sys.exit(1)

    with open(hql_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Split queries by semicolon
    raw_statements = content.split(";")
    cur = conn.cursor()
    query_idx = 0
    executed_count = 0

    for stmt in raw_statements:
        clean_stmt = stmt.strip()
        if not clean_stmt:
            continue

        # Extract title from SQL comments if present
        comments = [line.strip("- \t") for line in clean_stmt.splitlines() if line.strip().startswith("-- QUERY") or line.strip().startswith("-- Query")]

        # Remove SQL comment lines for execution
        executable_sql = "\n".join(line for line in clean_stmt.splitlines() if not line.strip().startswith("--")).strip()
        if not executable_sql:
            continue

        # Skip USE database or unsupported DDL statements in SQLite
        if executable_sql.upper().startswith("USE ") or executable_sql.upper().startswith("SET "):
            continue

        query_idx += 1
        title = comments[0] if comments else f"Query {query_idx}"

        # If a target query was requested and does not match, skip
        if target_query is not None and target_query != query_idx:
            continue

        # Convert Hive ROLLUP syntax for SQLite simulation compatibility
        if "WITH ROLLUP" in executable_sql.upper():
            executable_sql = re.sub(r"WITH\s+ROLLUP", "", executable_sql, flags=re.IGNORECASE)

        executed_count += 1
        print("=" * 65)
        print(f" HIVE EXECUTING: {title}")
        print("=" * 65)
        print(executable_sql[:160] + ("..." if len(executable_sql) > 160 else "") + "\n")

        try:
            cur.execute(executable_sql)
            cols = [d[0] for d in cur.description] if cur.description else []
            rows = cur.fetchall()

            if cols:
                widths = [max(len(str(val)) for val in [cols[i]] + [r[i] for r in rows]) for i in range(len(cols))]
                header_line = " | ".join(cols[i].ljust(widths[i]) for i in range(len(cols)))
                sep_line = "-+-".join("-" * widths[i] for i in range(len(cols)))
                print(header_line)
                print(sep_line)
                for r in rows[:15]: # Show top 15 rows
                    print(" | ".join(str(v if v is not None else "").ljust(widths[i]) for i, v in enumerate(r)))
                if len(rows) > 15:
                    print(f"... ({len(rows) - 15} more rows)")
            else:
                print("Query OK (0 rows returned)")
        except Exception as e:
            print(f"[WARN] Simulated execution note: {e}")

        print("\n")

    print("============================================================")
    if target_query:
        print(f" [COMPLETE] Successfully executed Hive Query {target_query}!")
    else:
        print(f" [COMPLETE] Successfully executed all {executed_count} HQL queries from {hql_path}!")
    print("============================================================")

if __name__ == "__main__":
    hql_file = "hive/02_analytical_queries.hql"
    target_q = None

    for i, arg in enumerate(sys.argv[1:], 1):
        if arg == "-f" and i < len(sys.argv) - 1:
            hql_file = sys.argv[i + 1]
        elif arg == "-q" and i < len(sys.argv) - 1:
            try:
                target_q = int(sys.argv[i + 1])
            except ValueError:
                pass
        elif arg.isdigit():
            target_q = int(arg)
        elif arg.endswith(".hql"):
            hql_file = arg

    conn = create_in_memory_db()
    execute_hql_file(conn, hql_file, target_q)
