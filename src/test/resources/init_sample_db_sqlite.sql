PRAGMA foreign_keys = ON;

--
-- Database: `yorm`
--

DROP TABLE IF EXISTS company;
CREATE TABLE company
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name          VARCHAR(20) NOT NULL,
    country_code  VARCHAR(2)  NOT NULL,
    creation_date DATE        NOT NULL,
    debt          FLOAT DEFAULT 0 NULL,
    is_active     TINYINT     NOT NULL,
    company_type  TEXT        NOT NULL CHECK(company_type IN ('GREEDY', 'NOT_GREEDY')) NOT NULL,
    is_evil       BOOL        NOT NULL
);

DROP TABLE IF EXISTS person;
CREATE TABLE person
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name       VARCHAR(20) NOT NULL,
    email      VARCHAR(55) NOT NULL,
    last_login TIMESTAMP   NOT NULL,
    company_id INT(10)     NOT NULL,
    FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE
);


DROP TABLE IF EXISTS invoice;
CREATE TABLE invoice
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    subject    VARCHAR(20) NOT NULL,
    amount     FLOAT DEFAULT 0 NULL,
    company_id INT(10) NOT NULL,
    FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS history_annotation;
CREATE TABLE history_annotation
(
    subject         VARCHAR(20) NOT NULL,
    amount          FLOAT DEFAULT 0 NULL,
    annotation_time TIME NULL,
    content         TEXT
);

DROP VIEW IF EXISTS person_company;
CREATE VIEW person_company AS
SELECT p.name, p.email, c.debt, c.is_active
FROM person p
         INNER JOIN company c ON p.company_id = c.id;
