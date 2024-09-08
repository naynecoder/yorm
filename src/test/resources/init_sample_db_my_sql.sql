SET
SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET
AUTOCOMMIT = 0;
START TRANSACTION;
SET
time_zone = "+00:00";

--
-- Database: `yorm`
--

DROP
DATABASE IF EXISTS yorm;
CREATE
DATABASE yorm;

USE
yorm;

DROP TABLE IF EXISTS person;
CREATE TABLE person
(
    id         INT(10) AUTO_INCREMENT PRIMARY KEY NOT NULL,
    name       VARCHAR(20) NOT NULL,
    email      VARCHAR(55) NOT NULL,
    last_login DATETIME    NOT NULL,
    company_id INT(10)     NOT NULL
);

DROP TABLE IF EXISTS company;
CREATE TABLE company
(
    id            INT(10) AUTO_INCREMENT PRIMARY KEY NOT NULL,
    name          VARCHAR(20) NOT NULL,
    country_code  VARCHAR(2)  NOT NULL,
    creation_date DATE        NOT NULL,
    debt          FLOAT DEFAULT 0 NULL,
    is_active     TINYINT     NOT NULL,
    company_type  ENUM('GREEDY', 'NOT_GREEDY') NOT NULL,
    is_evil       BOOL        NOT NULL
);

ALTER TABLE `person`
    ADD CONSTRAINT `person_id` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`) ON DELETE CASCADE;

DROP TABLE IF EXISTS invoice;
CREATE TABLE invoice
(
    id         INT(10) AUTO_INCREMENT PRIMARY KEY NOT NULL,
    subject    VARCHAR(20) NOT NULL,
    amount     FLOAT DEFAULT 0 NULL,
    company_id INT(10) NOT NULL
);

ALTER TABLE `invoice`
    ADD CONSTRAINT `invoice_id` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`) ON DELETE CASCADE;

DROP TABLE IF EXISTS history_annotation;
CREATE TABLE history_annotation
(
    subject         VARCHAR(20) NOT NULL,
    amount          FLOAT DEFAULT 0 NULL,
    annotation_time TIME NULL,
    content         TEXT,
    side_note       CHAR(20) NOT NULL
);

DROP VIEW IF EXISTS person_company;
CREATE VIEW person_company AS
SELECT p.name, p.email, c.debt, c.is_active
FROM person p
         INNER JOIN company c ON p.company_id = c.id;
