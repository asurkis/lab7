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
                    "CREATE TABLE IF NOT EXISTS lab7_users (" +
                            "id SERIAL," +
                            "email VARCHAR NOT NULL," +
                            "password VARCHAR NOT NULL)"
            );
            statement.execute();
            statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS lab7 (" +
                            "name VARCHAR NOT NULL," +
                            "size REAL NOT NULL," +
                            "position_x REAL NOT NULL," +
                            "position_y REAL NOT NULL," +
                            "creation_date TIMESTAMPTZ NOT NULL," +
                            "user_id INTEGER NOT NULL)");
            statement.execute();
        }
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public List<CollectionElement> show(int userId) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            List<CollectionElement> result = new ArrayList<>();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM lab7 WHERE user_id = ?");
            statement.setInt(1, userId);
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
    public CollectionInfo info(int userId) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM lab7 WHERE user_id = ?");
            statement.setInt(1, userId);
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
    public void addElement(CollectionElement element, int userId) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO lab7 " +
                    "(name, size, position_x, position_y, creation_date, user_id)" +
                    "VALUES (?, ?, ?, ?, ?, ?)");
            statement.setString(1, element.getName());
            statement.setDouble(2, element.getSize());
            statement.setDouble(3, element.getPosition().getX());
            statement.setDouble(4, element.getPosition().getY());
            statement.setTimestamp(5, Timestamp.valueOf(element.getCreationDate()));
            statement.setInt(6, userId);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeElement(CollectionElement element, int userId) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM lab7 WHERE " +
                            "name = ? AND " +
                            "size = ? AND " +
                            "position_x = ? AND " +
                            "position_y = ? AND " +
                            "user_id = ?");
            statement.setString(1, element.getName());
            statement.setDouble(2, element.getSize());
            statement.setDouble(3, element.getPosition().getX());
            statement.setDouble(4, element.getPosition().getY());
            statement.setInt(5, userId);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addUser(String email, String userPassword) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO lab7_users " +
                    "(email, password)" +
                    "VALUES (?, ?)");
            statement.setString(1, email);
            statement.setString(2, userPassword);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean checkUser(String email, String userPassword) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(id) FROM lab7_users WHERE " +
                    "email = ? AND " +
                    "password = ?");
            statement.setString(1, email);
            statement.setString(2, userPassword);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) != 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void removeFirst(int user_id) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM lab7 WHERE name IN (" +
                            "SELECT name FROM lab7 WHERE user_id = ? ORDER BY size DESC LIMIT 1)");
            statement.setInt(1, user_id);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeLast(int userId) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM lab7 WHERE name IN (" +
                            "SELECT name FROM LAB7 WHERE user_id = ? ORDER BY size ASC LIMIT 1)");
            statement.setInt(1, userId);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getUserId(String email, String passwordHash) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM lab7_users WHERE " +
                    "email = ? AND " +
                    "password = ?");
            statement.setString(1, email);
            statement.setString(2, passwordHash);
            System.out.println("Password -> " + passwordHash);
            ResultSet rs = statement.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public boolean consistsUser(String email) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(id) FROM lab7_users WHERE " +
                    "email = ?");
            statement.setString(1, email);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) != 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
