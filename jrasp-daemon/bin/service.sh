#!/bin/sh

while true
do
  process=$(ps aux | grep jrasp-daemon | grep -v grep)
  // issue 41
  if [ -z "$process" ] || [[ $process == *"jrasp-daemon.log"* ]]; then
    echo "jrasp-daemon is not running"
    ./jrasp-daemon
  else
    sleep 10
    echo "jrasp-daemon is running"
  fi
done
