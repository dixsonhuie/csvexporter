#!/usr/bin/env bash

PROJ_DIR="/home/dixson/work/proj20/csvexporter"

export GS_LOOKUP_LOCATORS="localhost:4174"
export GS_LOOKUP_GROUPS="xap-15.2.0"

java -jar $PROJ_DIR/exporter/target/csvexporter.jar --runClientSide --spaceName=mySpace --exportBaseDir=/tmp

# Or
## java -jar $PROJ_DIR/exporter/target/csvexporter.jar --runServerSide --spaceName=mySpace --exportBaseDir=/tmp