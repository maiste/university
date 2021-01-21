# POCA 2020
Our product is a marketplace connecting buyers to sellers. Similar products are: Amazon.com, Rakuten, Cdiscount.com, Veepee...

Online shopping is not original at all but it has a rich domain with interesting choices to make. Let's view it as a playground where we can either borrow ideas from our competitors or build our own vision of what a marketplace should be!

## Install instructions

To run the software locally, Docker and postgresql is needed.

In addition, scala, sbt and terraform are needed for development.

### Create the database

To connect: `sudo -u postgres psql`

```
postgres=# create database poca;
CREATE DATABASE
postgres=# create user poca with encrypted password 'poca';
CREATE ROLE
postgres=# grant all privileges on database poca to poca;
GRANT
postgres=# \connect poca
You are now connected to database "poca" as user "postgres".
poca=# alter schema public owner to poca;
ALTER SCHEMA
```

In `pg_hba.conf`, make sure there is a way to connect as poca:
* `local poca poca md5` to connect using `psql`
* `host poca poca 127.0.0.1/32 md5` to connect using TCP.

Restart the database. Test the connection with `psql poca poca`.

If you plan to run tests, you need to create another database `pocatest`.


## Run the tests

```
sbt clean coverage test coverageReport
```

This also creates a coverage report at [target/scala-2.13/scoverage-report/index.html](target/scala-2.13/scoverage-report/index.html).

## Add or update a table

To add a new table or update a previous one, you have to follow this steps:

1. Create a new file in the directory [src/main/scala/migrations](src/main/scala/migrations) with the name *Migration<ID>Action*.
2. In this file, add a new class that extends the Migration class
Ex:
```scala
package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._


class Migration<ID>Action(db: Database) extends Migration with LazyLogging {
    override def apply() {
      // Modification in the database
    }
}
```
3. Insert it into the migration list, line 10, in the [Migration.scala](src/main/scala/migrations/Migration.scala) file.

### Run locally using the Docker image from Docker Hub

```
docker run poca/poca-2020:latest
```

### Run from the local directory

```
sbt run
```

Then visit `http://localhost:8080/hello`

## Package to a Docker image

```
sbt docker:publishLocal
```

Then the image with name `poca-2020` and tag `latest` is listed. (There is also an image `poca-2020:0.1.0-SNAPSHOT` that is identical).

```
docker image ls
```

Run the docker image locally:

```
docker run --net=host poca-2020:latest
```

To remove old images:

```
docker image prune
```
