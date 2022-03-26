package org.yorm;

import static junit.framework.TestCase.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yorm.exception.YormException;
import org.yorm.records.Company;
import org.yorm.records.Person;
import org.yorm.utils.TestConnectionFactory;
import org.yorm.utils.TestDbHelper;

@Disabled
class PerformanceTest {

    private static Logger logger = LoggerFactory.getLogger(MapBuilder.class);
    private static DataSource ds;
    private static Yorm yorm;

    @BeforeAll
    private static void initDb() {
        ds = TestConnectionFactory.getConnection();
        yorm = new Yorm(ds);
    }

    @Test
    void testPerformance() throws YormException {
        Company company = new Company(1, "randomString", "ZZ", LocalDate.now(), 0,true);
        yorm.insert(company);
        List<Long> metricsInsertingYorm = new ArrayList<>();
        List<Long> metricsFindingYorm = new ArrayList<>();
        List<Long> metricsDeletingYorm = new ArrayList<>();
        int cycles = 15;
        int operations = 55;
        for (int op = 0; op < cycles; op++) {
            Instant time1 = Instant.now();
            massiveInsertWithYorm(operations);
            Instant time2 = Instant.now();
            metricsInsertingYorm.add(Duration.between(time1, time2).toMillis());
            Instant time3 = Instant.now();
            massiveGetWithYorm(operations, company);
            Instant time4 = Instant.now();
            metricsFindingYorm.add(Duration.between(time3, time4).toMillis());
            Instant time5 = Instant.now();
            massiveDeletionWithYorm(operations);
            Instant time6 = Instant.now();
            metricsDeletingYorm.add(Duration.between(time5, time6).toMillis());
        }
        List<Long> metricsInsertingJdbc = new ArrayList<>();
        List<Long> metricsFindingJdbc = new ArrayList<>();
        List<Long> metricsDeletingJdbc = new ArrayList<>();
        for (int op = 0; op < cycles; op++) {
            Instant time1 = Instant.now();
            massiveInsertWithJdbc(operations);
            Instant time2 = Instant.now();
            metricsInsertingJdbc.add(Duration.between(time1, time2).toMillis());
            Instant time3 = Instant.now();
            massiveGetWithJdbc(operations, company);
            Instant time4 = Instant.now();
            metricsFindingJdbc.add(Duration.between(time3, time4).toMillis());
            Instant time5 = Instant.now();
            massiveDeletionWithJdbc(operations);
            Instant time6 = Instant.now();
            metricsDeletingJdbc.add(Duration.between(time5, time6).toMillis());
        }
        long averageInsertYorm = getAverage(metricsInsertingYorm);
        long averageInsertJdbc = getAverage(metricsInsertingJdbc);
        long averageDeleteYorm = getAverage(metricsDeletingYorm);
        long averageDeleteJdbc = getAverage(metricsDeletingJdbc);
        long averageFindingYorm = getAverage(metricsFindingYorm);
        long averageFindingJdbc = getAverage(metricsFindingJdbc);
        logger.info("Average Inserting: yorm: {} ms - Jdbc: {} ms", averageInsertYorm, averageInsertJdbc);
        logger.info("Average Finding: yorm {} ms - Jdbc: {} ms", averageFindingYorm, averageFindingJdbc);
        logger.info("Average Deleting: yorm {} ms - Jdbc: {} ms", averageDeleteYorm, averageDeleteJdbc);
        long maxInserting = 35;
        long maxFinding = 68;
        long maxDeleting = 20;
        assertTrue(getPercentage(averageInsertYorm, averageInsertJdbc) < maxInserting);
        assertTrue(getPercentage(averageFindingYorm, averageFindingJdbc) < maxFinding);
        assertTrue(getPercentage(averageDeleteYorm, averageDeleteJdbc) < maxDeleting);


    }

    long getAverage(List<Long> list) {
        return list.stream().reduce(Long::sum).get() / list.size();
    }

    long getPercentage(long value1, long value2) {
        return 100 - value2 * 100 / value1;
    }

    public void massiveInsertWithYorm(int operations) throws YormException {
        String str = "randomString";
        for (int k = 1; k < operations; k++) {
            yorm.insert(new Person(k, str, str, LocalDateTime.now(), 1));
        }
    }

    public void massiveDeletionWithYorm(int operations) throws YormException {
        for (int k = 1; k < operations; k++) {
            yorm.delete(Person.class, k);
        }
    }

    public void massiveGetWithYorm(int operations, Company company) throws YormException {
        for (int k = 1; k < operations; k++) {
            yorm.find(Person.class, company);
        }
    }

    public void massiveInsertWithJdbc(int operations) throws YormException {
        String str = "randomString";
        for (int k = 1; k < operations; k++) {
            TestDbHelper.insertPerson(ds, k, str, str, LocalDateTime.now(), 1);
        }
    }

    public void massiveDeletionWithJdbc(int operations) throws YormException {
        for (int k = 1; k < operations; k++) {
            TestDbHelper.deletePerson(ds, k);
        }
    }

    public void massiveGetWithJdbc(int operations, Company company) throws YormException {
        for (int k = 1; k < operations; k++) {
            TestDbHelper.get(ds, company.id());
        }
    }

}
