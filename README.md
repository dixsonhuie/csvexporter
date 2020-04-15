# csvexporter

csvexporter can be used to export data from Gigaspaces to csv files.

## How I build it

Download this git repository and run `mvn package`.

## How I run it

1. Start a XAP cluster. Go to the Gigaspaces install directory and run:

`./gs.sh host run-agent --manager --gsc=4`

2. Deploy a space 'mySpace'

`./gs.sh space deploy --partitions=2 --ha mySpace`

3. In the exporter module there are some scripts that can be run from the command line.

See: exporter/src/main/resoources/bin/csvexporter.sh

The script runs the class `com.samples.demo.Demo`. It accepts the following command-line parameters:

* `--runClientSide` run this program on the client side.
* `--runServerSide` run this program on the server side.
* `--spaceName=<SPACE NAME>` the name of the space to connect to.
* `--exportBaseDir=</PATH/TO/EXPORT/DIR>` the parent directory where the exported .csv files are written. Default is /tmp.

If runClientSide is used, then the export is done on the client.

If runServerSide is used, then a Task is sent to Gigaspaces and the export is done on the server. See: https://docs.gigaspaces.com/latest/dev-java/task-execution-overview.html

4. The Gigaspaces CsvReader can be used to read the csv file and import the objects into the space. See: https://docs.gigaspaces.com/latest/started/importing-data.html




