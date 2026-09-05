#!/usr/bin/env bash
# ====================================================================
# SafeRoads: Hive Query Runner
# Usage:
#   bash run_hive.sh 1     # Runs Query 1 only
#   bash run_hive.sh 2     # Runs Query 2 only
#   ...
#   bash run_hive.sh 12    # Runs Query 12 only
#   bash run_hive.sh all   # Runs all 12 queries
# ====================================================================

bash hive/run_hive.sh "$@"
