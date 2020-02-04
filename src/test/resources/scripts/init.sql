CREATE SCHEMA IF NOT EXISTS consignmentapi;
CREATE TABLE IF NOT EXISTS consignmentapi.Series (
  SeriesId int NOT NULL,
  BodyId int DEFAULT NULL,
  Name varchar(255) DEFAULT NULL,
  Code varchar(255) DEFAULT NULL,
  Description varchar(255) DEFAULT NULL,
  PRIMARY KEY (SeriesId)
)

