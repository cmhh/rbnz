# Create a Database and Data Service of RBNZ Statistics

Data on the [Reserve Bank of New Zealand](https://www.rbnz.govt.nz/statistics) website can be a little awkward to source and use.  First, all data is stored in Excel spreasheets which aren't directly machine readable.  But even then the data isn't easily used because the files themselves, due to the way they're hosted, cannot be directly downloaded easily in tools such as R or Python.  For example, the following R command will fail outright (as will `curl` and `wget`):

```r
download.file(
  "https://www.rbnz.govt.nz/-/media/ReserveBank/Files/Statistics/tables/b1/hb1-daily.xlsx?revision=1ad7d513-7a05-469a-891a-068829a9661a",
  "hb1-daily.xlsx
)
```

This repo includes a simple-ish Scala library which has entrypoints which:

* download all Excel files listed on the [RBNZ statistics page](https://www.rbnz.govt.nz/statistics)
* import most Excel files and output as a SQLite database
* run a basic data service on top of the resulting SQLite database.

Note that Excel files will successfully import if they:

* have a tab named `Data`
* have a tab named `Series Definitions`
* data in `Data` tab must start in row 6, with series IDs in row 5
* `Series Definitions` tab must have 5 columns with header row

The resulting database is basic, with the following schema:

![](img/relationships.real.large.png)


## Build

It is assumed users have [sbt](https://www.scala-sbt.org/).  All that is required to build the library is to run the following command:

```bash
sbt assembly
```

This will yield the following artefact:

```plaintext
target/scala-2.13/rbnz.jar
```

## Selenium / Chrome / Chromedriver

The program uses Selenium webdriver, and assumes Chrome and [chromedriver](https://chromedriver.chromium.org/) are available, and working correctly.  One easy way to ensure this is the case is to use Docker, and so a sufficient `Dockerfile` is provided.

## Using the Library with Docker

Users first need to build the docker image:

```bash
docker build -t rbnz .
```

(Note that `sbt assembly` must have been run previously, so that `target/scala-2.13/rbnz.jar` exists.)

### Download Data

To download all the Excel files to a local directory:

```bash
docker run --rm \
  -u $(id -u):$(id -g) \
  -v ${PWD}/data:/data rbnz \
  org.cmhh.DownloadData data
```

At the time this was written, this was roughly 120 files, totalling only 9.5 MB, or 8.1MB zipped.  While this is a small amount of data, the download does pause for around 5 seconds between each file so as not to add any real pressure to the remote server.  This does mean the data can take a few minutes to download, however.

### Create Database 

We can create a SQLite database as follows:

```bash
docker run --rm \
  -u $(id -u):$(id -g) \
  -v ${PWD}/data:/data -v ${PWD}/output:/output \
  rbnz org.cmhh.CreateDatabase output/rbnz.db
```

If files already exist in `${PWD}/data`, they will not be downloaded again.  To get updated files, either clear the folder, or mount a different, empty folder.

### Run Data Service

Finally, to run the service:

```bash
docker run -td --rm \
  -v ${PWD}/output:/output \
  -p 9001:9001 \
  rbnz org.cmhh.Service output/rbnz.db
```

The data service is simple, with just two end-points:

* `/rbnz/definition` - list available series IDs
* `/rbnz/series` - list all observations for requested series

Each end-point takes 3 arguments:

* `id` - series ID, optional, repeating
* `groupKeyword` - search for text in group name, optional, repeating
* `nameKeywork` - search for text in name, optional, repeating.

For example, `GET`ting:

```plaintext
http://localhost:9001/rbnz/definition?groupKeyword=Exchange&nameKeyword=European
```

yields

```json
[
    {
        "group": "Exchange rates (quoted per NZ$)",
        "id": "EXR.DS11.D03",
        "name": "European euro",
        "unit": "NZD/EUR",
        "frequency": "D",
        "note": null
    },
    {
        "group": "Exchange rates (quoted per NZ$)",
        "id": "EXR.MS11.D03",
        "name": "European euro",
        "unit": "NZD/EUR",
        "frequency": "M",
        "note": null
    }
]
```
 
The service uses a single SQLite connection, though it would be easy enough to add some sort of connection pool, but is nevertheless reasonably fast.  For example:

```bash
$ siege -t 10s -c 4 "http://localhost:9001/rbnz/series?id=EXRT.MR41.NZB17"
```
```plaintext
** SIEGE 4.0.7
** Preparing 4 concurrent users for battle.
The server is now under siege...
Lifting the server siege...
Transactions:                   3222 hits
Availability:                 100.00 %
Elapsed time:                   9.91 secs
Data transferred:              34.08 MB
Response time:                  0.01 secs
Transaction rate:             325.13 trans/sec
Throughput:                     3.44 MB/sec
Concurrency:                    3.97
Successful transactions:        3222
Failed transactions:               0
Longest transaction:            0.06
Shortest transaction:           0.00
```

It's a little slower for larger series, but still reasonable.  `EXRT.DS41.NZB17`, for example, has 5860 or so observations, and it is returned in about 15 milliseconds.  

```bash
$ siege -t 10s -c 2 "http://localhost:9001/rbnz/series?id=EXRT.DS41.NZB17"
```
```plaintext
** SIEGE 4.0.7
** Preparing 2 concurrent users for battle.
The server is now under siege...
Lifting the server siege...
Transactions:                    482 hits
Availability:                 100.00 %
Elapsed time:                   9.10 secs
Data transferred:              50.95 MB
Response time:                  0.04 secs
Transaction rate:              52.97 trans/sec
Throughput:                     5.60 MB/sec
Concurrency:                    1.99
Successful transactions:         482
Failed transactions:               0
Longest transaction:            0.14
Shortest transaction:           0.02
```

Users could then write wrappers in other languages.  For example, using R we can plot the daily TWI easily enough:

```r
library(jsonlite)

twi <- jsonlite::fromJSON(
  "http://localhost:9001/rbnz/series?id=EXRT.DS41.NZB17", 
  simplifyDataFrame = FALSE
)

plot(
  twi[[1]]$date |> as.Date(), 
  sapply(twi[[1]]$value, \(x) if (is.null(x)) NA else x), 
  type = "l", 
  xlab = "date", ylab = twi[[1]]$unit,
  main = sprintf("%s, %s", twi[[1]]$group, twi[[1]]$name)
)
```

![](img/twi.png)