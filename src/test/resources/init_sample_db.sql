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

DROP DATABASE IF EXISTS yorm;
CREATE DATABASE yorm;

USE yorm;

DROP TABLE IF EXISTS person;
CREATE TABLE person
(
    id         INT(10) AUTO_INCREMENT PRIMARY KEY NOT NULL,
    name       VARCHAR(20) NOT NULL,
    email      VARCHAR(55) NOT NULL,
    company_id int(10) NOT NULL
);

DROP TABLE IF EXISTS company;
CREATE TABLE company
(
    id            INT(10) AUTO_INCREMENT PRIMARY KEY NOT NULL,
    name          VARCHAR(20) NOT NULL,
    country_code  VARCHAR(2)  NOT NULL,
    creation_date DATE        NOT NULL,
    is_active     TINYINT     NOT NULL
);

ALTER TABLE `person`
    ADD CONSTRAINT `person_id` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`) ON DELETE CASCADE;


