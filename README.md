## Consignment API 

This is the API which accesses the TDR consignment database. 

### Schema
The schema of the database is managed in this [project](https://github.com/nationalarchives/tdr-consignment-api-data)

### Building locally
The auth utils and generated slick classes libraries are now stored in a private bucket in S3. This means you will need aws credentials to download the dependencies.
You need either a default profile set up or you need to set the AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables. 

### Running locally

Set up the database
```
docker run --name postgres -p 5432:5432 -e POSTGRES_USER=tdr -e POSTGRES_PASSWORD=password -e POSTGRES_DB=consignmentapi -d postgres:11.6
git clone https://github.com/nationalarchives/tdr-consignment-api-data.git
cd tdr-consignment-api-data.git
sbt flywayMigrate
```

Run the api:

* From IntelliJ, run the ApiServer app
* Or from the command line, run `sbt run`

#### Generate Graphql Schema Locally

To generate the Graphql schema locally run the following command:

`sbt graphqlSchemaGen`

The generated schema file will be placed in the following location: target/sbt-graphql/schema.graphql
