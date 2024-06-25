package org.yorm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.yorm.utils.TestConnectionFactory;

public interface DBCompatibilityTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class MySQLTest extends BaseDBCompatibilityTest {
        @BeforeAll
        public void setUp() {
            ds = TestConnectionFactory.getMySqlConnection();
            yorm = new Yorm(ds);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class PostgreSQLTest extends BaseDBCompatibilityTest {
        @BeforeAll
        public void setUp() {
            ds = TestConnectionFactory.getPostgreSqlConnection();
            yorm = new Yorm(ds);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class SQLiteTest extends BaseDBCompatibilityTest {
        @BeforeAll
        public void setUp() {
            ds = TestConnectionFactory.getSQLiteConnection();
            yorm = new Yorm(ds);
        }
    }
}
