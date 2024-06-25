package org.yorm.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import java.util.stream.Stream;

public class TestConnectionFactory {

    private static DataSource mySqlDs;

    private static DataSource postgreSqlDs;

    private static DataSource sqliteSqlDs;

    private TestConnectionFactory() {
    }

    public synchronized static DataSource getMySqlConnection() {
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

    public synchronized static DataSource getPostgreSqlConnection() {
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

    public synchronized static DataSource getSQLiteConnection() {
        if (sqliteSqlDs == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:file:sharedmemdb?mode=memory&cache=shared");
            config.setConnectionInitSql("PRAGMA foreign_keys = ON;");
            sqliteSqlDs = new HikariDataSource(config);

            try (
                    Connection connection = sqliteSqlDs.getConnection();
                    Statement initStatement = connection.createStatement();
                    InputStream initScriptStream = classLoader.getResourceAsStream("init_sample_db_sqlite.sql");
                    Scanner scanner = new Scanner(initScriptStream);
            ) {
                String initScript = scanner.useDelimiter("\\A").next();

                // TODO: Java 21 has a method called splitWithDelimiters() which would save us from adding
                // the ";" back on.
                String[] statements = initScript.split(";");
                for (String statement : statements) {
                    statement = statement.trim() + ";";
                    if (!";".equals(statement)) {
                        initStatement.execute(statement);
                    }
                }
            } catch (SQLException | IOException e) {
                throw new RuntimeException("Error creating SQLite in memory DB.", e);
            }
        }
        return sqliteSqlDs;
    }
}
