CREATE TABLE IF NOT EXISTS Series (
  BodyId UUID DEFAULT '6e3b76c4-1745-4467-8ac5-b4dd736e1b3e',
  Name varchar(255) DEFAULT NULL,
  Code varchar(255) DEFAULT NULL,
  Description varchar(255) DEFAULT NULL,
  SeriesId UUID NOT NULL DEFAULT '6e3b76c4-1745-4467-8ac5-b4dd736e1b3e',
  PRIMARY KEY (SeriesId)
);

CREATE TABLE IF NOT EXISTS Body (
   BodyId UUID not null DEFAULT '6e3b76c4-1745-4467-8ac5-b4dd736e1b3e',
   Name varchar(255) default null,
   Code varchar(255) default null,
   Description varchar(255) default null,
   PRIMARY KEY (BodyId)
);

CREATE TABLE IF NOT EXISTS Consignment (
  ConsignmentId uuid NOT NULL DEFAULT '6e3b76c4-1745-4467-8ac5-b4dd736e1b3e',
  SeriesId uuid DEFAULT '6e3b76c4-1745-4467-8ac5-b4dd736e1b3e',
  UserId uuid DEFAULT NULL,
  Datetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ParentFolder varchar(255),
  PRIMARY KEY (ConsignmentId)
);

CREATE TABLE IF NOT EXISTS TransferAgreement (
  ConsignmentId uuid NOT NULL DEFAULT '6e3b76c4-1745-4467-8ac5-b4dd736e1b3e',
  AllPublicRecords BOOLEAN DEFAULT NULL,
  AllCrownCopyright BOOLEAN DEFAULT NULL,
  AllEnglish BOOLEAN DEFAULT NULL,
  AllDigital BOOLEAN DEFAULT NULL,
  AppraisalSelectionSignedOff BOOLEAN DEFAULT NULL,
  SensitivityReviewSignedOff BOOLEAN DEFAULT NULL,
  TransferAgreementId uuid NOT NULL DEFAULT '6e3b76c4-1745-4467-8ac5-b4dd736e1b3e',
PRIMARY KEY (TransferAgreementId)
);

CREATE TABLE IF NOT EXISTS ClientFileMetadata (
   FileId uuid NOT NULL DEFAULT '6e3b76c4-1745-4467-8ac5-b4dd736e1b3e',
   OriginalPath varchar(255) DEFAULT NULL,
   Checksum varchar(255) DEFAULT NULL,
   ChecksumType varchar(255) DEFAULT NULL,
   LastModified timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   CreatedDate timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
   Filesize bigint DEFAULT NULL,
   Datetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
   ClientFileMetadataId uuid NOT NULL DEFAULT '6e3b76c4-1745-4467-8ac5-b4dd736e1b3e',
   PRIMARY KEY (ClientFileMetadataId)
);

CREATE TABLE IF NOT EXISTS File (
   FileId uuid NOT NULL DEFAULT '6e3b76c4-1745-4467-8ac5-b4dd736e1b3e',
   ConsignmentId uuid NOT NULL,
   UserId uuid DEFAULT NULL,
   Datetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY (FileId)
);

CREATE TABLE IF NOT EXISTS AVMetadata (
    FileId uuid DEFAULT NULL,
    Software varchar(255) NOT NULL,
    SoftwareVersion varchar(255) NOT NULL,
    DatabaseVersion varchar(255) NOT NULL,
    Result varchar(255) NOT NULL,
    Datetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS FileProperty (
    PropertyId uuid,
    Name varchar(255),
    Description varchar(255),
    Shortname varchar(255),
    PRIMARY KEY (PropertyId)
);

CREATE TABLE IF NOT EXISTS FileMetadata (
    MetadataId uuid,
    FileId uuid not null,
    PropertyId uuid not null,
    Value varchar(255) not null,
    Datetime timestamp not null DEFAULT CURRENT_TIMESTAMP,
    UserId uuid NOT NULL,
    PRIMARY KEY (MetadataId)
);

 ALTER TABLE FileMetadata
    ADD FOREIGN KEY (FileId)
    REFERENCES File(FileId);

ALTER TABLE FileMetadata
    ADD FOREIGN KEY (PropertyId)
    REFERENCES FileProperty(PropertyId);
    
CREATE TABLE IF NOT EXISTS FFIDMetadata (
    FFIDMetadataId uuid not null,
    FileId uuid NOT NULL,
    Software varchar(255) not null,
    SoftwareVersion varchar(255) not null,
    BinarySignatureFileVersion varchar(255) not null,
    ContainerSignatureFileVersion varchar(255) not null,
    Method varchar(255) not null,
    Datetime timestamp not null,
    PRIMARY KEY(FFIDMetadataId)
);

CREATE TABLE IF NOT EXISTS FFIDMetadataMatches (
    FFIDMetadataId uuid not null,
    Extension varchar(255),
    IdentificationBasis varchar(255) not null,
    PUID varchar(255) not null
);

 ALTER TABLE FFIDMetadataMatches
     ADD FOREIGN KEY (FFIDMetadataId)
     REFERENCES FFIDMetadata(FFIDMetadataId);

 ALTER TABLE FFIDMetadata
    ADD FOREIGN KEY (FileId)
    REFERENCES File(FileId);


DELETE from Body;
INSERT INTO Body (BodyId, Name, Code, Description) VALUES ('6e3b76c4-1745-4467-8ac5-b4dd736e1b3e', 'Body', 'Code', 'Description'), ('645bee46-d738-439b-8007-2083bc983154', 'Body2', 'Code2', 'Description');
