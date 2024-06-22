-- SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
-- SET AUTOCOMMIT = 0;
-- START TRANSACTION;
-- SET time_zone = "+00:00";

--
-- Database: "yorm"
--

-- DROP DATABASE IF EXISTS yorm;
-- CREATE DATABASE yorm;

-- USE yorm;

DROP TABLE IF EXISTS person;
CREATE TABLE person
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(20) NOT NULL,
    email      VARCHAR(55) NOT NULL,
    last_login TIMESTAMP   NOT NULL,
    company_id int         NOT NULL
);

DROP TABLE IF EXISTS company;
CREATE TABLE company
(
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(20) NOT NULL,
    country_code  VARCHAR(2)  NOT NULL,
    creation_date DATE        NOT NULL,
    debt          REAL DEFAULT 0 NULL,
    is_active     BOOLEAN     NOT NULL,
    company_type  VARCHAR(10) NOT NULL,
    is_evil       BOOLEAN     NOT NULL
);

ALTER TABLE "person"
    ADD CONSTRAINT "person_id" FOREIGN KEY ("company_id") REFERENCES "company" ("id") ON DELETE CASCADE;

DROP TABLE IF EXISTS invoice;
CREATE TABLE invoice
(
    id         SERIAL PRIMARY KEY,
    subject    VARCHAR(20) NOT NULL,
    amount     FLOAT DEFAULT 0 NULL,
    company_id int         NOT NULL
);

ALTER TABLE "invoice"
    ADD CONSTRAINT "invoice_id" FOREIGN KEY ("company_id") REFERENCES "company" ("id") ON DELETE CASCADE;

DROP TABLE IF EXISTS history_annotation;
CREATE TABLE history_annotation
(
    subject         VARCHAR(20) NOT NULL,
    amount          REAL DEFAULT 0 NULL,
    annotation_time TIME NULL,
    content         TEXT
);

DROP VIEW IF EXISTS person_company;
CREATE VIEW person_company AS
SELECT p.name, p.email, c.debt, c.is_active
FROM person p
         INNER JOIN company c ON p.company_id = c.id;
