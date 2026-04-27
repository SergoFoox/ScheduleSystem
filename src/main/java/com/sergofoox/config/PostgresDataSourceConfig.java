package com.sergofoox.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("postgres")
public class PostgresDataSourceConfig {

    @Bean
    public DataSource dataSource(Environment environment) {
        String databaseUrl = firstPresent(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL")
        );

        ParsedDatabaseUrl parsedUrl = databaseUrl == null
                ? fromPgEnvironment(environment)
                : parseDatabaseUrl(databaseUrl);

        String username = firstPresent(
                environment.getProperty("SPRING_DATASOURCE_USERNAME"),
                environment.getProperty("PGUSER"),
                parsedUrl.username()
        );
        String password = firstPresent(
                environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
                environment.getProperty("PGPASSWORD"),
                parsedUrl.password()
        );

        if (username == null || password == null) {
            throw new IllegalStateException("PostgreSQL credentials are missing. Set PGUSER/PGPASSWORD or SPRING_DATASOURCE_USERNAME/SPRING_DATASOURCE_PASSWORD.");
        }

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
        return new ParsedDatabaseUrl("jdbc:postgresql://" + host + ":" + port + "/" + database, null, null);
    }

    private static ParsedDatabaseUrl parseDatabaseUrl(String databaseUrl) {
        if (databaseUrl.startsWith("jdbc:")) {
            return new ParsedDatabaseUrl(databaseUrl, null, null);
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

        String username = null;
        String password = null;
        if (uri.getUserInfo() != null) {
            String[] parts = uri.getUserInfo().split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }

        return new ParsedDatabaseUrl(jdbcUrl, username, password);
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

    private record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}
