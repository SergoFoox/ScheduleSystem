package com.sergofoox.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
@ConditionalOnProperty(name = "spring.datasource.driverClassName", havingValue = "org.postgresql.Driver")
public class PostgresDataSourceConfig {

    private static final String INVALID_CATALOG_SQL_STATE = "3D000";
    private static final String DUPLICATE_DATABASE_SQL_STATE = "42P04";

    @Bean
    public DataSource dataSource(Environment environment) {
        String databaseUrl = firstPresent(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("spring.datasource.url")
        );

        ParsedDatabaseUrl parsedUrl = databaseUrl == null
                ? fromPgEnvironment(environment)
                : parseDatabaseUrl(databaseUrl);

        String username = firstPresent(
                environment.getProperty("SPRING_DATASOURCE_USERNAME"),
                environment.getProperty("PGUSER"),
                parsedUrl.username(),
                environment.getProperty("spring.datasource.username")
        );
        String password = firstPresent(
                environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
                environment.getProperty("PGPASSWORD"),
                parsedUrl.password(),
                environment.getProperty("spring.datasource.password")
        );

        if (username == null || password == null) {
            throw new IllegalStateException("PostgreSQL credentials are missing. Set PGUSER/PGPASSWORD or SPRING_DATASOURCE_USERNAME/SPRING_DATASOURCE_PASSWORD.");
        }

        ensureDatabaseExists(parsedUrl, username, password);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(parsedUrl.jdbcUrl());
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private static ParsedDatabaseUrl fromPgEnvironment(Environment environment) {
        String host = environment.getProperty("PGHOST");
        String database = environment.getProperty("PGDATABASE");
        if (host == null || database == null) {
            throw new IllegalStateException("PostgreSQL connection is missing. Set DATABASE_URL or PGHOST/PGDATABASE.");
        }

        String port = environment.getProperty("PGPORT", "5432");
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        String maintenanceJdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";
        return new ParsedDatabaseUrl(jdbcUrl, maintenanceJdbcUrl, database, null, null);
    }

    private static ParsedDatabaseUrl parseDatabaseUrl(String databaseUrl) {
        if (databaseUrl.startsWith("jdbc:")) {
            return parseJdbcDatabaseUrl(databaseUrl);
        }

        URI uri = URI.create(databaseUrl);
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalStateException("Unsupported DATABASE_URL scheme: " + scheme);
        }

        String path = uri.getPath() == null || uri.getPath().isBlank() ? "/postgres" : uri.getPath();
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + port + path + query;
        String maintenanceJdbcUrl = "jdbc:postgresql://" + uri.getHost() + port + "/postgres" + query;
        String databaseName = decode(path.substring(1));

        String username = null;
        String password = null;
        if (uri.getUserInfo() != null) {
            String[] parts = uri.getUserInfo().split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }

        return new ParsedDatabaseUrl(jdbcUrl, maintenanceJdbcUrl, databaseName, username, password);
    }

    private static ParsedDatabaseUrl parseJdbcDatabaseUrl(String jdbcUrl) {
        String withoutPrefix = jdbcUrl.substring("jdbc:".length());
        URI uri = URI.create(withoutPrefix);
        String path = uri.getPath() == null || uri.getPath().isBlank() ? "/postgres" : uri.getPath();
        String databaseName = decode(path.substring(1));
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        String maintenanceJdbcUrl = "jdbc:postgresql://" + uri.getHost() + port + "/postgres" + query;
        return new ParsedDatabaseUrl(jdbcUrl, maintenanceJdbcUrl, databaseName, null, null);
    }

    private static void ensureDatabaseExists(ParsedDatabaseUrl parsedUrl, String username, String password) {
        try (Connection ignored = DriverManager.getConnection(parsedUrl.jdbcUrl(), username, password)) {
            return;
        } catch (SQLException targetException) {
            if (!INVALID_CATALOG_SQL_STATE.equals(targetException.getSQLState())) {
                throw new IllegalStateException("Failed to connect to PostgreSQL database: " + parsedUrl.jdbcUrl(), targetException);
            }
        }

        try (Connection connection = DriverManager.getConnection(parsedUrl.maintenanceJdbcUrl(), username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE " + quoteIdentifier(parsedUrl.databaseName()));
        } catch (SQLException createException) {
            if (!DUPLICATE_DATABASE_SQL_STATE.equals(createException.getSQLState())) {
                throw new IllegalStateException("PostgreSQL database '" + parsedUrl.databaseName() + "' does not exist and could not be created.", createException);
            }
        }
    }

    private static String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalStateException("PostgreSQL database name is missing.");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record ParsedDatabaseUrl(String jdbcUrl, String maintenanceJdbcUrl, String databaseName, String username, String password) {
    }
}
