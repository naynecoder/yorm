package org.yorm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.yorm.exception.YormException;
import org.yorm.records.Company;
import org.yorm.records.Person;
import org.yorm.util.DbType;
import org.yorm.utils.TestConnectionFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class YormTest {

    private static DataSource ds;
    private static Yorm yorm;

    @BeforeAll
    private static void initDb() {
        ds = TestConnectionFactory.getConnection();
        yorm = new Yorm(ds);
    }

    @Test
    @Order(1)
    void buildMap() {
        MapBuilder mp = new MapBuilder(ds);
        YormTable map = mp.buildMap(Person.class);
        assertNotNull(map);
        List<YormTuple> tuples = map.getTuples();
        assertEquals(4, tuples.size());
        YormTuple tuple0 = tuples.get(0);
        assertEquals("id", tuple0.dbFieldName());
        assertEquals(DbType.INT, tuple0.type());
        assertEquals("id", tuple0.method().getName());
        assertEquals("id", tuple0.objectName());
        assertEquals("PRI", tuple0.key());
        YormTuple tuple4 = tuples.get(3);
        assertEquals("company_id", tuple4.dbFieldName());
        assertEquals(DbType.INT, tuple0.type());
        assertEquals("companyId", tuple4.method().getName());
        assertEquals("companyId", tuple4.objectName());
        assertEquals("company_id", tuple4.dbFieldName());
        assertEquals("MUL", tuple4.key());
    }

    @Test
    @Order(2)
    void saveCompany() throws YormException {
        Company company = new Company(0, "Hogwarts", "GB", LocalDate.of(1968, 2, 12), true);
        int id = yorm.save(company);
        assertEquals(1, id);
        Company company2 = new Company(0, "Mordor", "ZZ", LocalDate.of(114, 11, 5), false);
        int id2 = yorm.save(company);
        assertEquals(2, id2);
    }

    @Test
    @Order(3)
    void savePerson() throws YormException {
        Person person = new Person(0, "John", "john.doe@um.com", 1);
        int idPerson = yorm.save(person);
        assertEquals(1, idPerson);
        Person personWrong = new Person(0, "John", "john.doe@um.com", 0);
        assertThrows(YormException.class, () -> yorm.save(personWrong));
    }

    @Test
    @Order(4)
    void saveListPersons() throws YormException {
        Person person1 = new Person(2, "Hermione", "hermione.granger@hogwarts.com", 1);
        Person person2 = new Person(3, "Harry", "harry.potter@hogwarts.com", 1);
        Person person3 = new Person(4, "Sauron", "sauron@mordor.com", 2);
        List<Person> list = List.of(person1, person2, person3);
        yorm.insert(list);
        List<Person> personList = yorm.get(Person.class);
        assertNotNull(personList);
        assertEquals(4, personList.size());
    }

    @Test
    @Order(5)
    void getCompany() throws YormException {
        Company company = yorm.get(Company.class, 1);
        assertEquals("GB", company.countryCode());
        assertEquals("Hogwarts", company.name());
        assertEquals(LocalDate.of(1968, 2, 12), company.date());
        assertTrue(company.isActive());
    }

    @Test
    @Order(6)
    void getPerson() throws YormException {
        Person person = yorm.get(Person.class, 1);
        assertEquals("John", person.name());
        assertEquals("john.doe@um.com", person.email());
        assertEquals(1, person.companyId());
        Person person2 = yorm.get(Person.class, 2);
        assertEquals("Hermione", person2.name());
        assertEquals("hermione.granger@hogwarts.com", person2.email());
        assertEquals(1, person2.companyId());
    }

    @Test
    @Order(7)
    void getEverything() throws YormException {
        List<Person> personList = yorm.get(Person.class);
        assertNotNull(personList);
        assertEquals(4, personList.size());
        Person person2 = personList.get(1);
        assertEquals("Hermione", person2.name());
        assertEquals("hermione.granger@hogwarts.com", person2.email());
        assertEquals(1, person2.companyId());
    }

    @Test
    @Order(8)
    void getWithForeignKey() throws YormException {
        Company company = new Company(1, null, null, null, false);
        List<Person> personList = yorm.get(Person.class, company);
        assertNotNull(personList);
        assertEquals(3, personList.size());
        Person person2 = personList.get(1);
        assertEquals("Hermione", person2.name());
        assertEquals("hermione.granger@hogwarts.com", person2.email());
        assertEquals(1, person2.companyId());
        List<Person> personList2 = yorm.get(Person.class, new Company(2, null, null, null, false));
        assertNotNull(personList2);
        assertEquals(1, personList2.size());
        Person person1 = personList2.get(0);
        assertEquals("Sauron", person1.name());
        assertEquals("sauron@mordor.com", person1.email());
        assertEquals(2, person1.companyId());
    }

    @Test
    @Order(9)
    void getFiltering() throws YormException {
        Person personFilter1 = new Person(0, "harry", "john", 0);
        Person personFilter2 = new Person(0, null, null, 2);
        List<Person> list = List.of(personFilter1, personFilter2);
        List<Person> personList = yorm.get(list);
        assertNotNull(personList);
        assertEquals(3, personList.size());
        Person person1 = personList.stream().filter(p -> p.id() == 4).findFirst().get();
        assertEquals(4, person1.id());
        assertEquals("Sauron", person1.name());
        assertEquals("sauron@mordor.com", person1.email());
        assertEquals(2, person1.companyId());
    }

    @Test
    @Order(10)
    void getFilteringNoResults() throws YormException {
        Person personFilter1 = new Person(0, "Peter", null, 0);
        List<Person> personList = yorm.get(List.of(personFilter1));
        assertTrue(personList.isEmpty());
    }

    @Test
    @Order(11)
    void update() throws YormException {
        Person personResultBefore = yorm.get(Person.class, 1);
        assertEquals(1, personResultBefore.id());
        assertEquals("John", personResultBefore.name());
        assertEquals("john.doe@um.com", personResultBefore.email());
        assertEquals(1, personResultBefore.companyId());
        Person person = new Person(1, "Draco", "draco.malfoy@hogwarts.com", 1);
        yorm.save(person);
        Person personResult = yorm.get(Person.class, 1);
        assertEquals(1, personResult.id());
        assertEquals("Draco", personResult.name());
        assertEquals("draco.malfoy@hogwarts.com", personResult.email());
        assertEquals(1, personResult.companyId());
    }

    @Test
    @Order(12)
    void delete() throws YormException {
        yorm.delete(Person.class, 1);
        Person personResult = yorm.get(Person.class, 1);
        assertNull(personResult);
    }
}
