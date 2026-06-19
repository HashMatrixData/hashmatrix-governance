package io.hashmatrix.governance.domain.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 锁定**契约线上形态**：{@code AssetType} 序列化为小写串、检索结果是**裸契约结构**（无 ApiResponse 信封）。
 * 若有人给契约端点加了信封或改了枚举大小写，这里立刻红。
 */
class MetadataSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void assetTypeSerializesToLowercaseWire() throws Exception {
        assertThat(mapper.writeValueAsString(AssetType.TABLE)).isEqualTo("\"table\"");
        assertThat(mapper.writeValueAsString(AssetType.API)).isEqualTo("\"api\"");
    }

    @Test
    void searchResultSerializesToRawContractShape() throws Exception {
        MetaSearchResult result =
                new MetaSearchResult(
                        List.of(
                                new AssetSummary(
                                        "asset-1001", "orders", AssetType.TABLE, "data-team",
                                        List.of("finance"))),
                        1,
                        20,
                        1,
                        List.of(new Facet("type", List.of(new FacetBucket("table", 1)))));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(result));

        // 顶层即契约 MetaSearchResult，绝无 {code,message,data} 信封
        assertThat(json.has("code")).isFalse();
        assertThat(json.has("data")).isFalse();
        assertThat(json.path("items").get(0).path("type").asText()).isEqualTo("table");
        assertThat(json.path("items").get(0).path("id").asText()).isEqualTo("asset-1001");
        assertThat(json.path("page").asInt()).isEqualTo(1);
        assertThat(json.path("total").asInt()).isEqualTo(1);
        assertThat(json.path("facets").get(0).path("field").asText()).isEqualTo("type");
        assertThat(json.path("facets").get(0).path("buckets").get(0).path("value").asText())
                .isEqualTo("table");
        assertThat(json.path("facets").get(0).path("buckets").get(0).path("count").asInt())
                .isEqualTo(1);
    }
}
