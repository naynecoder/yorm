package org.yorm.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public class TestConnectionFactory {

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    static {
        config.setJdbcUrl("jdbc:tc:mysql:5.7.34:///databasename/yorm?TC_INITSCRIPT=file:src/test/resources/init_sample_db.sql");
        config.setUsername("root");
        config.setPassword("test");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(config);
    }

    private TestConnectionFactory() {
    }

    public static DataSource getConnection() {
        return ds;
    }

}
