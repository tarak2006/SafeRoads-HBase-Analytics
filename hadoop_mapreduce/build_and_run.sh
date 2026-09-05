#!/usr/bin/env bash
# ====================================================================
# SafeRoads: Java MapReduce Build and Hadoop Cluster Execution Script
# Compiles all 4 Java MapReduce jobs, packages SafeRoads.jar, and
# provides automated execution commands on Hadoop HDFS.
# ====================================================================

set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$BASE_DIR"

mkdir -p build

echo "============================================================"
echo " 1. Compiling Java MapReduce Sources with Hadoop Classpath"
echo "============================================================"

# Check if Hadoop is available on PATH
if command -v hadoop &> /dev/null; then
    HADOOP_CP=$(hadoop classpath)
    echo "[INFO] Using Hadoop classpath from system."
    javac -classpath "$HADOOP_CP" -d ./build src/saferoads/mapreduce/*.java
    jar -cvfe SafeRoads.jar saferoads.mapreduce.SafeRoadsDriver -C ./build/ .
    echo "[SUCCESS] Packaged SafeRoads.jar successfully!"
else
    echo "[WARN] 'hadoop' command not found in current PATH."
    echo "To compile on your college Hadoop cluster or lab machine, run:"
    echo "  javac -classpath \$(hadoop classpath) -d ./build src/saferoads/mapreduce/*.java"
    echo "  jar -cvfe SafeRoads.jar saferoads.mapreduce.SafeRoadsDriver -C ./build/ ."
fi

echo ""
echo "============================================================"
echo " 2. Execution Commands on Hadoop Cluster"
echo "============================================================"
echo "# Step A: Upload preprocessed dataset to HDFS:"
echo "  hdfs dfs -mkdir -p /user/bigdata/saferoads/input"
echo "  hdfs dfs -put -f ../data/processed/accidents_cleaned.tsv /user/bigdata/saferoads/input/"
echo ""
echo "# Step B: Run Job 1 (State & Hourly Risk Matrix):"
echo "  hadoop jar SafeRoads.jar hourlyRisk /user/bigdata/saferoads/input /user/bigdata/saferoads/out_job1"
echo ""
echo "# Step C: Run Job 2 (Weather vs Average Severity):"
echo "  hadoop jar SafeRoads.jar weatherSeverity /user/bigdata/saferoads/input /user/bigdata/saferoads/out_job2"
echo ""
echo "# Step D: Run Job 3 (Infrastructure Hazard Correlation):"
echo "  hadoop jar SafeRoads.jar infrastructure /user/bigdata/saferoads/input /user/bigdata/saferoads/out_job3"
echo ""
echo "# Step E: Run Job 4 (Extreme Delay Top-K via Secondary Sorting):"
echo "  hadoop jar SafeRoads.jar durationTopK /user/bigdata/saferoads/input /user/bigdata/saferoads/out_job4"
echo ""
echo "# Step F: View Results from HDFS:"
echo "  hdfs dfs -cat /user/bigdata/saferoads/out_job1/part-r-00000 | head -n 20"
echo "============================================================"
