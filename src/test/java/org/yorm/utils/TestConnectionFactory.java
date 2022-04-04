package org.yorm.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public class TestConnectionFactory {

    private static DataSource mySqlDs;

    private static DataSource postgreSqlDs;

    private TestConnectionFactory() {
    }

    public static DataSource getMySqlConnection() {
        if (mySqlDs == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:tc:mysql:5.7.34:///databasename/yorm?TC_INITSCRIPT=file:src/test/resources/init_sample_db_my_sql.sql");
            config.setUsername("root");
            config.setPassword("test");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            mySqlDs = new HikariDataSource(config);
        }
        return mySqlDs;
    }

    public static DataSource getPostgreSqlConnection() {
        if(postgreSqlDs == null){
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:tc:postgresql:9.6.8:///databasename/yorm?TC_INITSCRIPT=file:src/test/resources/init_sample_db_postgre_sql.sql");
            config.setUsername("root");
            config.setPassword("test");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            postgreSqlDs = new HikariDataSource(config);
        }
        return postgreSqlDs;
    }

}
