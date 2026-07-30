package com.paperpilot.server.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaMaintenanceConfig {

    @Bean
    CommandLineRunner paperRecordSchemaMaintenance(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE paper_record MODIFY paper_url LONGTEXT");
            } catch (Exception ignored) {
                // The table may not exist yet on first boot; Hibernate will create it from the entity mapping.
            }
            try {
                jdbcTemplate.execute("ALTER TABLE paper_record ADD COLUMN source_url LONGTEXT");
            } catch (Exception ignored) {
                // Column already exists or table is not ready yet.
            }
            try {
                jdbcTemplate.execute("ALTER TABLE paper_record ADD COLUMN import_source VARCHAR(128)");
            } catch (Exception ignored) {
                // Column already exists or table is not ready yet.
            }
            try {
                jdbcTemplate.execute("ALTER TABLE forum_post MODIFY content LONGTEXT");
            } catch (Exception ignored) {
                // Existing databases may already have a large enough content column.
            }
            try {
                jdbcTemplate.execute("ALTER TABLE friend_request ADD COLUMN contact_info VARCHAR(255)");
            } catch (Exception ignored) {
                // Column already exists or table is not ready yet.
            }
        };
    }
}
