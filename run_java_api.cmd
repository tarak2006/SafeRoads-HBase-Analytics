@echo off
REM SafeRoads HBase Review 2 - Java API Demonstration Runner
echo ====================================================================
echo   Running SafeRoads HBase Java API Demonstration
echo ====================================================================
wsl -d Ubuntu -e bash -c "cd /mnt/e/Big_data/big_data_14 && HBASE_CLASSPATH=/mnt/e/Big_data/big_data_14/hbase/target/classes /usr/local/hbase/bin/hbase saferoads.hbase.SafeRoadsHBaseManager /mnt/e/Big_data/big_data_14/data/processed/accidents_cleaned.tsv 2000"
