## Consignment API

This is the API which accesses the TDR consignment database. 

### Schema
The schema of the database is managed in this [project](https://github.com/nationalarchives/tdr-consignment-api-data)

### Running locally

Set up the database
```
docker run --name mysql -e MYSQL_ROOT_PASSWORD=password -e MYSQL_DATABASE=consignmentapi -d -p 3306:3306 mysql:5.7
git clone https://github.com/nationalarchives/tdr-consignment-api-data.git
cd tdr-consignment-api-data.git
sbt flywayMigrate
```

Run the api

`sbt run`

#### Generate Graphql Schema Locally

To generate the Graphql schema locally run the following command:

`sbt graphqlSchemaGen`

The generated schema file will be placed in the following location: target/sbt-graphql/schema.graphql