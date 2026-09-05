#!/usr/bin/env bash
# ====================================================================
# SafeRoads: MapReduce Single-Job & Multi-Job Runner
# Usage:
#   bash run_mapreduce.sh 1    # Runs Job 1 (Hourly Risk)
#   bash run_mapreduce.sh 2    # Runs Job 2 (Weather Severity)
#   bash run_mapreduce.sh 3    # Runs Job 3 (Infrastructure Hazards)
#   bash run_mapreduce.sh 4    # Runs Job 4 (Top-K Duration)
#   bash run_mapreduce.sh all  # Runs all 4 jobs
# ====================================================================

JOB="${1:-all}"
DATASET="${2:-data/processed/accidents_cleaned.tsv}"

if [ ! -f "$DATASET" ] && [ -f "data/processed/accidents_cleaned_sample.tsv" ]; then
    DATASET="data/processed/accidents_cleaned_sample.tsv"
fi

java -cp hadoop_mapreduce/build:hadoop_mapreduce/lib/hadoop-core-stubs.jar saferoads.mapreduce.LocalJavaMapReduceRunner "$JOB" "$DATASET"
