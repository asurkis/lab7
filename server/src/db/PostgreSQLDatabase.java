package db;

import collection.CollectionElement;
import collection.CollectionInfo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class PostgreSQLDatabase implements Database {
    private Connection connection;

    public PostgreSQLDatabase(String uri, String user, String password) throws SQLException {
        connection = DriverManager.getConnection(uri, user, password);
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    @Override
    public List<CollectionElement> show() {
        try {
            connection.beginRequest();
            connection.endRequest();
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public CollectionInfo info() {
        return null;
    }

    @Override
    public void addElement(CollectionElement element) {

    }

    @Override
    public void removeElement(CollectionElement element) {

    }

    @Override
    public void removeFirst() {

    }

    @Override
    public void removeLast() {

    }
}
