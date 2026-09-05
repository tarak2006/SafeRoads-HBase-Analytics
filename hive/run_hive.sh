#!/usr/bin/env bash
# ====================================================================
# SafeRoads: Hive HQL Runner Script
# Runs raw .hql query files directly as SQL:
# Usage:
#   bash hive/run_hive.sh -f hive/02_analytical_queries.hql
# ====================================================================

python3 hive/hive_cli.py "$@"
