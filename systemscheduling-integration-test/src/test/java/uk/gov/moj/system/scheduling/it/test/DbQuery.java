package uk.gov.moj.system.scheduling.it.test;

import static java.lang.String.format;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class DbQuery {
    private String select;
    private String from;
    private String[] filterColumns;
    private Object[] parameters;

    public static DbQuery newQuery() {
        return new DbQuery();
    }

    public DbQuery selectAll() {
        this.select = "*";
        return this;
    }

    public DbQuery from(String tableName) {
        this.from = tableName;
        return this;
    }

    public DbQuery filterOn(String... columnNames) {
        this.filterColumns = columnNames;
        return this;
    }

    public DbQuery withParameters(Object... parameters) {
        this.parameters = parameters;
        return this;
    }

    public Map<String, Object> queryWithRetry(final Connection connection, final int retry, final long sleepTime) throws Exception {
        return queryWithRetry(connection, retry, sleepTime, map -> !map.isEmpty());
    }

    public Map<String, Object> queryWithRetry(final Connection connection, final int retry, final long sleepTime, final Predicate<Map<String, Object>> predicate) throws Exception {
        Map<String, Object> queryResult = new HashMap<>();

        for (int i = 0; i < retry; i++) {
            queryResult = this.query(connection);
            if (!predicate.test(queryResult)) {
                Thread.sleep(sleepTime);
            } else {
                break;
            }
        }
        return queryResult;
    }

    public Map<String, Object> query(Connection connection) throws SQLException {
        Map<String, Object> queryResult = new HashMap<>();
        String statement = format(" SELECT %s FROM %s  %s", select, from, buildWhereClause());
        PreparedStatement preparedStatement = connection.prepareStatement(statement);
        int j = 1;
        for (Object param : parameters) {
            preparedStatement.setObject(j, param);
            j++;
        }
        ResultSet rs = preparedStatement.executeQuery();
        while (rs.next()) {
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                queryResult.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
            }
        }
        return queryResult;
    }

    private String buildWhereClause() {
        String clause = " WHERE ";
        for (String column : filterColumns) {
            clause += column + " = ? AND ";
        }
        if (clause.endsWith("AND ")) {
            clause = clause.substring(0, clause.lastIndexOf("AND"));
        }
        return clause;
    }
}
