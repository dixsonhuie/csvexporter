set PROJ_DIR=C:/Users/dixson/work/gigaspace/csvexporter

set GS_LOOKUP_LOCATORS=localhost:4174
set GS_LOOKUP_GROUPS=xap-15.2.0

java -jar %PROJ_DIR%/exporter/target/csvexporter.jar --runClientSide --spaceName=mySpace --exportBaseDir=/tmp

# Or
## java -jar %PROJ_DIR%/exporter/target/csvexporter.jar --runServerSide --spaceName=mySpace --exportBaseDir=/tmp