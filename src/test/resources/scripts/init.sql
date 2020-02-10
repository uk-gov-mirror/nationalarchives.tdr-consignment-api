CREATE SCHEMA IF NOT EXISTS consignmentapi;
CREATE TABLE IF NOT EXISTS consignmentapi.Series (
  BodyId int DEFAULT NULL,
  Name varchar(255) DEFAULT NULL,
  Code varchar(255) DEFAULT NULL,
  Description varchar(255) DEFAULT NULL,
  SeriesId int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (SeriesId)
);
CREATE TABLE IF NOT EXISTS consignmentapi.Body (
   BodyId int not null,
   Name varchar(255) default null,
   Code varchar(255) default null,
   Description varchar(255) default null,
   PRIMARY KEY (BodyId)
);
DELETE from consignmentapi.Body;
INSERT INTO consignmentapi.Body (BodyId, Name, Code, Description) VALUES (1, 'Body', 'Code', 'Description'), (2, 'Body2', 'Code', 'Description');
