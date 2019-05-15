package db;

import collection.CollectionElement;
import collection.CollectionInfo;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostgreSQLDatabase implements Database {
    private String uri;
    private String user;
    private String password;

    public PostgreSQLDatabase(String uri, String user, String password) throws SQLException {
        this.uri = uri;
        this.user = user;
        this.password = password;

        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS LAB7 (" +
                            "NAME VARCHAR NOT NULL," +
                            "SIZE REAL NOT NULL," +
                            "POSITION_X REAL NOT NULL," +
                            "POSITION_Y REAL NOT NULL," +
                            "CREATION_DATE TIMESTAMPTZ NOT NULL" +
                            "user_id NOT NULL REFERENCES lab7_users(id)");
            statement.execute();
            statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS lab7_users (" +
                            "id INT PRIMARY KEY NOT NULL AUTOINCREMENT," +
                            "email VARCHAR NOT NULL," +
                            "password VARCHAR NOT NULL)"
            );
            statement.execute();
        }
    }

    @Override
    public void close() throws Exception {}

    @Override
    public List<CollectionElement> show(int user_id) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            List<CollectionElement> result = new ArrayList<>();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM LAB7 WHERE user_id = ?");
            statement.setInt(1, user_id);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                double size = rs.getDouble("size");
                double posX = rs.getDouble("position_x");
                double posY = rs.getDouble("position_y");
                Timestamp creationDate = rs.getTimestamp("creation_date");
                result.add(new CollectionElement(name, size, posX, posY)
                        .withCreationDate(creationDate.toLocalDateTime()));
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public CollectionInfo info(int user_id) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM LAB7 WHERE user_id = ?");
            statement.setInt(1, user_id);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new CollectionInfo(LocalDateTime.MIN, rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void addElement(CollectionElement element, int user_id) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO LAB7" +
                    "(NAME, SIZE, POSITION_X, POSITION_Y, CREATION_DATE, user_id)" +
                    "VALUES (?, ?, ?, ?, ?, ?)");
            statement.setString(1, element.getName());
            statement.setDouble(2, element.getSize());
            statement.setDouble(3, element.getPosition().getX());
            statement.setDouble(4, element.getPosition().getY());
            statement.setTimestamp(5, Timestamp.valueOf(element.getCreationDate()));
            statement.setInt(6, user_id);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeElement(CollectionElement element, int user_id) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM LAB7 WHERE " +
                            "NAME = ? AND " +
                            "SIZE = ? AND " +
                            "POSITION_X = ? AND " +
                            "POSITION_Y = ? AND " +
                            "user_id = ?");
            statement.setString(1, element.getName());
            statement.setDouble(2, element.getSize());
            statement.setDouble(3, element.getPosition().getX());
            statement.setDouble(4, element.getPosition().getY());
            statement.setInt(5, user_id);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addUser (String email, String password) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO lab7_users" +
                    "(email, password)" +
                    "VALUES (?, ?)");
            statement.setString(1, email);
            statement.setString(2, password);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean checkUser(String email, String password) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(id) FROM lab7_users WHERE" +
                    "email = ? AND " +
                    "password = ?");
            statement.setString(1, email);
            statement.setString(2, password);
            statement.execute();
            return (statement.getFetchSize() != 0 ? true : false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void removeFirst(int user_id) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM LAB7 WHERE NAME IN (" +
                            "SELECT NAME FROM LAB7 WHERE user_id = ? ORDER BY SIZE DESC LIMIT 1)");
            statement.setInt(1, user_id);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeLast(int user_id) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM LAB7 WHERE NAME IN (" +
                            "SELECT NAME FROM LAB7 WHERE user_id = ? ORDER BY SIZE ASC LIMIT 1)");
            statement.setInt(1, user_id);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getUserId(String email, String passoword) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM lab7_users WHERE" +
                    "email = ? AND " +
                    "password = ?");
            statement.setString(1, email);
            statement.setString(2, password);
            statement.execute();
            return statement.getResultSet().getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
