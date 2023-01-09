#!/bin/bash
cd $(dirname $0) || exit 1
cd ../
projectpath=`pwd`
rm ${projectpath}/bin/jrasp-attach
go clean
