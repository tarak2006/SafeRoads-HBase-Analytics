@echo off
REM Windows wrapper to forward hbase commands to WSL Ubuntu
wsl -d Ubuntu -e bash -c "cd /mnt/e/Big_data/big_data_14 && /usr/local/hbase/bin/hbase %*"
