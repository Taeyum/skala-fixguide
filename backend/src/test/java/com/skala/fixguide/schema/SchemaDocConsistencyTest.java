package com.skala.fixguide.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * API 명세서 7장 "DB 매핑" 표(docs/07_api/README.md)와 실제 DB 스키마의 컬럼 목록이 같은지 확인한다.
 * 문서를 파싱하지 않고, 표에 적힌 컬럼을 상수로 옮겨 information_schema 와 비교한다.
 * 엔티티에 컬럼을 추가·삭제하면 이 테스트가 먼저 깨지므로 그때 문서와 상수를 함께 고친다.
 */
@ActiveProfiles("test")
@SpringBootTest
class SchemaDocConsistencyTest {

    /** docs/07_api/README.md 7장 표 그대로 */
    private static final Map<String, List<String>> DOCUMENTED = new LinkedHashMap<>();

    static {
        DOCUMENTED.put("users", List.of(
                "id", "name", "email", "password_hash", "role", "created_at", "updated_at"));
        DOCUMENTED.put("work_requests", List.of(
                "id", "request_no", "requester_id", "equipment", "line", "substance", "operating_condition",
                "product_name", "product_type", "spec_json", "symptom", "site_memo", "engineer_note", "status",
                "submitted_at", "created_at", "updated_at"));
        DOCUMENTED.put("work_request_photos", List.of(
                "id", "work_request_id", "file_name", "storage_key", "thumbnail_key", "size", "uploaded_at"));
        DOCUMENTED.put("agent_runs", List.of(
                "id", "work_request_id", "status", "started_at", "finished_at", "input_snapshot", "ai_config_id",
                "created_at", "updated_at"));
        DOCUMENTED.put("agent_steps", List.of(
                "id", "run_id", "agent_code", "status", "message", "error_message", "started_at", "finished_at"));
        DOCUMENTED.put("agent_results", List.of(
                "id", "run_id", "agent_code", "payload_json", "original_json", "edited", "created_at", "updated_at"));
        DOCUMENTED.put("ai_configs", List.of(
                "id", "agent_code", "provider", "model_name", "prompt_version", "temperature", "max_tokens",
                "egress_allowed", "is_active", "created_at", "updated_at"));
        DOCUMENTED.put("approvals", List.of(
                "id", "work_request_id", "approver_id", "decision", "reason", "reason_category", "decided_at"));
    }

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("7장 DB 매핑 표의 테이블·컬럼이 실제 스키마와 정확히 일치한다")
    void documentedColumnsMatchActualSchema() throws Exception {
        Map<String, TreeSet<String>> actual = new LinkedHashMap<>();
        try (var conn = dataSource.getConnection();
                var ps = conn.prepareStatement("""
                        select table_name, column_name
                        from information_schema.columns
                        where table_schema = current_schema()
                        order by table_name, ordinal_position
                        """);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                actual.computeIfAbsent(rs.getString(1), k -> new TreeSet<>()).add(rs.getString(2));
            }
        }

        assertThat(actual.keySet()).as("테이블 목록").containsExactlyInAnyOrderElementsOf(DOCUMENTED.keySet());
        DOCUMENTED.forEach((table, columns) -> assertThat(actual.get(table))
                .as("%s 컬럼 (문서 %d개 vs 실제 %d개)", table, columns.size(), actual.get(table).size())
                .containsExactlyInAnyOrderElementsOf(columns));
    }
}
