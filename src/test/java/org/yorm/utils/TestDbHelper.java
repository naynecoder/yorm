package org.yorm.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.yorm.exception.YormException;
import org.yorm.records.Person;

public class TestDbHelper {

    public static void insertPerson(DataSource ds, int id, String str, String str2, LocalDateTime localDateTime, int id2) throws YormException {
        String query = "INSERT INTO person (id, name, email, last_login, company_id) VALUES(?,?,?,?,?)";
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, id);
            preparedStatement.setString(2, str);
            preparedStatement.setString(3, str2);
            preparedStatement.setTimestamp(4, Timestamp.valueOf(localDateTime));
            preparedStatement.setInt(5, id2);
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                id = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new YormException(e.getMessage());
        }
    }

    public static void deletePerson(DataSource ds, int id) throws YormException {
        String query = "DELETE FROM person WHERE id=?";
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new YormException(e.getMessage());
        }
    }

    public static List<Person> get(DataSource ds, int id) throws YormException {
        String query = "SELECT id, name, email, last_login, company_id FROM person WHERE company_id=?";
        List<Person> list = new ArrayList<>();
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                list.add(new Person(rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getTimestamp(4).toLocalDateTime(), rs.getInt(5)));
            }
        } catch (SQLException e) {
            throw new YormException(e.getMessage());
        }
        return list;
    }

}
