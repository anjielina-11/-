package com.yunong.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationTest {

    @Test
    void addsCompositeIndexesForHighFrequencyListQueries() throws Exception {
        var sql = readMigration("/db/migration/V3__optimize_query_indexes.sql");

        assertThat(sql)
                .contains("idx_dr_observation_created")
                .contains("idx_dr_review_created")
                .contains("idx_ft_assignee_status_created")
                .contains("idx_pc_creator_status_created")
                .contains("CREATE INDEX IF NOT EXISTS");
    }

    @Test
    void initializesAiIntegrationSchemaModelAndKnowledge() throws Exception {
        var sql = readMigration("/db/migration/V5__ai_integration.sql");

        assertThat(sql)
                .contains("class_mapping_path")
                .contains("num_classes")
                .contains("status = 'deployed'")
                .contains("status = 'deprecated'")
                .contains("status = 'training'")
                .contains("云农病害识别 ResNet50")
                .contains("/app/best_model.pth")
                .contains("/app/class_to_idx.pth")
                .contains("0.8387")
                .contains("INSERT INTO knowledge_documents")
                .contains("10000000-0000-0000-0000-000000000006")
                .contains("ON CONFLICT");
    }

    private String readMigration(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}