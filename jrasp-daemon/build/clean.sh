#!/bin/bash
cd $(dirname $0) || exit 1
cd ../
projectpath=`pwd`
rm ${projectpath}/bin/jrasp-daemon
go clean