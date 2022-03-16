# Yorm
## _Yet another ORM_

[![Build Status](https://travis-ci.org/joemccann/dillinger.svg?branch=master)](https://travis-ci.org/joemccann/dillinger)

Yorm is a basic ORM designed to work with Java Records.
In the world of microservices, there is a tendency to have very contained logic within every service,
and hence reduced databases, that in many cases are just no more than several tables with not that many fields.
Java Records usually are a perfect fit for basic CRUD operations, and here is where Yorm shines.

**Yorm** might be for you in case:

- You are working with microservices with Java and like Java Records
- Your relational databases are pretty simple and basic
- Your tables contain auto increment ids
- You don't need complex INNER JOIN queries, just basic CRUD

Although the Java industry offers very well maintained ORM solutions like Hibernate or Jooq, they tend not work that well 
with Java Records. **Yorm** on the other side is specifically designed to leverage this Java capability.

Due to the immutable nature of Java Recored, **Yorm** cannot be understood as a persistent ORM. 
## Features

- No need to generate classes
- No need to add annotations
- No need to write SQL for basic operations
- Seamless flow with API REST and CRUD operations

**Yorm** doesn't need to generate classes or to annotate them, but it works on conventions. It will assume that your
table has a Primary Key called *id*, probably with an autoincrement. Also it will assume that foreign keys will follow the naming patter of *table_id*. The convention will assume as well that fields in the table and fields in the record will have the same name, or a very similar one.

When a Java Record is operated with **Yorm**, a reflection inspection will came in, and all the methods of the Record will be matched with their counterparts from the database. This matching will be kept in memory as a map, to avoid using reflection again. However, if there is a change in the database, the microservice will probably need to be restarted to refresh this mapping.

## Dependencies

*Yorm* has been designed to need very few dependencies:

- [HikariCP] - Hikari, to deal with the database
- [MySql] - So far, the only database officially supported
- [Junit 5] - For the unit tests
- [TestContainers] - Also for the unit tests
- [log4j] - Logging is usually useful

And that's it, the *Yorm* lies heavily on Java 17 Records and Reflections.

## How to use it with examples

Imagine you have a database with a table called Person, defined like:

```sql
CREATE TABLE person
(
    id         INT(10) AUTO_INCREMENT PRIMARY KEY NOT NULL,
    name       VARCHAR(20) NOT NULL,
    email      VARCHAR(55) NOT NULL,
    company_id int(10) NOT NULL
);
```

Hence, you a Java Record that can match that table:

```java
public record Person(int id, String name, String email, int companyId) {}
```
Please note how *companyId* follows the traditional camelcase in Java, and *company_id* the underscore which is
very popular in the database world. It can be like that, *Yorm* will take care and match both fields.

Saving an object will be something as easy as:
```java
Person person = new Person(0, "John", "john.doe@um.com", 1);
int idPerson = yorm.save(person);
```
This will translate to SQL:
```sql
INSERT INTO person (name, email, company_id) VALUES ("John", "john.doe@um.com", 1)
```

**Yorm** will detect that the id has a 0, consider it an INSERT, and insert it in the database, in table Person. It will return the id of the object just inserted. 

We might need to insert the object with its id, there is no problem:
```java
Person person = new Person(2, "Mark", "mark.doe@um.com", 1);
int idPerson = yorm.insert(person);
```
This operation will be automatically translated into
```sql
INSERT INTO person (id, name, email, company_id) VALUES (2, "Mark", "mark.doe@um.com", 1)
```

The update operation follows the same pattern. Please bear in mind that Records are immutable, so we have to 
create a new one, and the id will be used to detect that it's an update operation:
```java
Person person = new Person(2, "Jacob", "jacob.doe@um.com", 1);
yorm.save(person);
```
Whose equivalent SQL would be:
```sql
UPDATE person SET name = "Jacob", email="jacob.doe@um.com" WHERE id=2 AND company_id=1
```
Why is using company_id here? Well, it looks like a candidate to be a foreign key to a company table:
```sql
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
```
**Yorm** will detect that there is one Primary Key and a Multiple Key, and use them for the update. Nevertheless, **Yorm** also allows the update operation:
```java
Person person = new Person(2, "Jacob", "jacob.doe@um.com", 1);
yorm.update(person);
```
We've inserted and updated elements in the table. How can we retrieve them into Records. The first and easiest way would be retrieving all the elements. It's just one line:
```java
List<Person> personList = yorm.get(Person.class);
```
Which is the same as the SQL:
```sql
SELECT id, name, email, company_id FROM person
```
And automatically wrapping the result into a List.
Of course, we could just get one element, if we know the id:
```java
Person person = yorm.get(Person.class, 1);
```
Which translates to SQL of:
```sql
SELECT id, name, email, company_id FROM person WHERE id=1
```
Or even retrieve elements with a foreign key:
```java
Company company = new Company(1, null, null, null, false);
List<Person> personList = yorm.get(Person.class, company);
```
Pay attention how the Record company, with id 1, is used to retrieve a list of objects Person whose company_id is 1. **Yorm** will detect that Person has a foreign key with Company and use that to build the query:
```sql
SELECT id, name, email, company_id FROM person WHERE company_id=1
```
Some kinds of filtering is also allowed:
```java
Person personFilter1 = new Person(0, "harry", "john", 0);
Person personFilter2 = new Person(0, null, null, 2);
List<Person> list = List.of(personFilter1, personFilter2);
List<Person> personList = yorm.get(list);
```
Let's review this one a bit. **Yorm** deals with Records. Here we have defined two Person records, the first one with some information on *name* and *email*, and the second one with a *company_id*. **Yorm** will ignore all the nulls and 0s on fields that are ids, but use the rest of the information to build a SELECT query and retrieve a List of Person Records:
```sql
SELECT id, name, email, company_id FROM person WHERE name like '%harry%' OR email like '%john%' OR company_id=2
```
As a final note, Yorm works just by creating an instance of Yorm with a *javax.sql.DataSource*:
```java
DataSource ds = DbConnector.getDatasource(parameters);
Yorm yorm = new Yorm(ds);
```
**Yorm** is very young but nevertheless quite useful if you just want to perform basic CRUD operations, specially the ones that involve REST endpoints in the world of microservices. It's very to useful, and very transparent, since you just need a DataSource to put it to work.
(optional) Third:

#### Building from source

**Yorm** is a library that uses Maven. It can be very easily compiled like

```sh
mvn clean install
```



## License

Apache 2.0


[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)

   [HikariCP]: <https://github.com/brettwooldridge/HikariCP>
   [Mysql]: <https://https://www.mysql.com>
   [Junit 5]: <https://junit.org/junit5/>
   [TestContainers]: <https://www.testcontainers.org/>
   [log4j]: <https://logging.apache.org/log4j/2.x/>
   
