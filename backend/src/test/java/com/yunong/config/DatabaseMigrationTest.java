package com.yunong.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationTest {

    @Test
    void addsCompositeIndexesForHighFrequencyListQueries() throws Exception {
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V3__optimize_query_indexes.sql")) {
            assertThat(input).isNotNull();
            var sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("idx_dr_observation_created")
                    .contains("idx_dr_review_created")
                    .contains("idx_ft_assignee_status_created")
                    .contains("idx_pc_creator_status_created")
                    .contains("CREATE INDEX IF NOT EXISTS");
        }
    }
}