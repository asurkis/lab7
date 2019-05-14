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
                            "CREATION_DATE TIMESTAMPTZ NOT NULL)");
            statement.execute();
        }
    }

    @Override
    public void close() throws Exception {}

    @Override
    public List<CollectionElement> show() {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            List<CollectionElement> result = new ArrayList<>();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM LAB7");
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
    public CollectionInfo info() {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM LAB7");
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
    public void addElement(CollectionElement element) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO LAB7" +
                    "(NAME, SIZE, POSITION_X, POSITION_Y, CREATION_DATE)" +
                    "VALUES (?, ?, ?, ?, ?)");
            statement.setString(1, element.getName());
            statement.setDouble(2, element.getSize());
            statement.setDouble(3, element.getPosition().getX());
            statement.setDouble(4, element.getPosition().getY());
            statement.setTimestamp(5, Timestamp.valueOf(element.getCreationDate()));
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeElement(CollectionElement element) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM LAB7 WHERE " +
                            "NAME = ? AND " +
                            "SIZE = ? AND " +
                            "POSITION_X = ? AND " +
                            "POSITION_Y = ?");
            statement.setString(1, element.getName());
            statement.setDouble(2, element.getSize());
            statement.setDouble(3, element.getPosition().getX());
            statement.setDouble(4, element.getPosition().getY());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //TODO table for users
    @Override
    public void add_user (String email, String password) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO ???" +
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
    public boolean check_user(String email, String password) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(id) FROM ??? WHERE" +
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

    //TODO order by?
    @Override
    public void removeFirst() {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM LAB7 WHERE id IN (" +
                            "SELECT id FROM LAB7 ORDER BY ??? DESC LIMIT 1)");
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //TODO order by?
    @Override
    public void removeLast() {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM LAB7 WHERE id IN (" +
                            "SELECT id FROM LAB7 ORDER BY ??? ASC LIMIT 1)");
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
