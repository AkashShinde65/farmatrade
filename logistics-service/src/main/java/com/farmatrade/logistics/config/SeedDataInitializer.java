package com.farmatrade.logistics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads cold_storage_seed.sql and truck_seed.sql exactly once each, only when the corresponding
 * table is currently empty. Replaces Spring's built-in spring.sql.init mechanism, which re-runs
 * data.sql-style scripts on every startup -- fine for a schema that's recreated each time, but
 * it silently duplicated every seeded row on every container restart against a persistent
 * MySQL volume, since neither seed file guards against being run twice.
 */
@Component
public class SeedDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final Resource coldStorageSeed;
    private final Resource truckSeed;

    public SeedDataInitializer(
            JdbcTemplate jdbcTemplate,
            @Value("classpath:cold_storage_seed.sql") Resource coldStorageSeed,
            @Value("classpath:truck_seed.sql") Resource truckSeed) {
        this.jdbcTemplate = jdbcTemplate;
        this.coldStorageSeed = coldStorageSeed;
        this.truckSeed = truckSeed;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        seedIfEmpty("cold_storage_facility", coldStorageSeed);
        seedIfEmpty("truck", truckSeed);
    }

    private void seedIfEmpty(String table, Resource seedFile) throws IOException {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        if (count != null && count > 0) {
            return;
        }
        String sql = stripComments(seedFile.getContentAsString(StandardCharsets.UTF_8));
        jdbcTemplate.execute(sql);
    }

    private String stripComments(String sql) {
        StringBuilder result = new StringBuilder();
        for (String line : sql.lines().toList()) {
            if (!line.trim().startsWith("--")) {
                result.append(line).append('\n');
            }
        }
        return result.toString();
    }
}
