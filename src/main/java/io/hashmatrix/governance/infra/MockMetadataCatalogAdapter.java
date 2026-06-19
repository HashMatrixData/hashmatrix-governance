package io.hashmatrix.governance.infra;

import io.hashmatrix.governance.domain.metadata.AssetSummary;
import io.hashmatrix.governance.domain.metadata.AssetType;
import io.hashmatrix.governance.domain.metadata.Facet;
import io.hashmatrix.governance.domain.metadata.FacetBucket;
import io.hashmatrix.governance.domain.metadata.MetaSearchResult;
import io.hashmatrix.governance.domain.metadata.SearchQuery;
import io.hashmatrix.governance.domain.port.MetadataCatalogPort;
import io.hashmatrix.starter.tenant.TenantContextHolder;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@link MetadataCatalogPort} 的 **M1 mock 适配器**：返回脱敏 mock 元数据，贴合契约
 * {@code governance-metadata-v1} 的 {@code /api/meta/search}，供 webui / tools-bi 联调消费。
 *
 * <p>租户经 {@link TenantContextHolder} 流过（审计/日志可见）。M1 对所有租户返回**同一脱敏目录**——
 * 真实**按租户隔离的 PG(JSONB)+ES 落库**见 #12（typedef-driven），届时新增对应 Adapter 即替换本类，
 * {@code api}/{@code app} 不动（端口隔离，基础骨架不返工）。
 *
 * <p>🔴 红线：种子一律虚构脱敏（{@code asset-*} 标识、虚构表名、{@code data-team} 等占位），无任何真实信息。
 */
@Component
public class MockMetadataCatalogAdapter implements MetadataCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(MockMetadataCatalogAdapter.class);

    /** 脱敏 mock 目录（虚构资产）。 */
    private static final List<AssetSummary> SEED =
            List.of(
                    new AssetSummary(
                            "asset-1001", "orders", AssetType.TABLE, "data-team",
                            List.of("finance", "core")),
                    new AssetSummary(
                            "asset-1002", "customers", AssetType.TABLE, "data-team",
                            List.of("crm")),
                    new AssetSummary(
                            "asset-1003", "order_items", AssetType.TABLE, "data-team",
                            List.of("finance")),
                    new AssetSummary(
                            "asset-1004", "daily_revenue", AssetType.VIEW, "bi-team",
                            List.of("finance", "report")),
                    new AssetSummary(
                            "asset-1005", "customer_360", AssetType.DATASET, "bi-team",
                            List.of("crm", "report")),
                    new AssetSummary(
                            "asset-1006", "orders_api", AssetType.API, "platform-team",
                            List.of("serving")),
                    new AssetSummary(
                            "asset-1007", "customer_email", AssetType.COLUMN, "data-team",
                            List.of("pii")));

    @Override
    public MetaSearchResult search(SearchQuery query) {
        String tenant = TenantContextHolder.getTenantId().orElse("public");
        log.debug("meta search tenant={} q={} type={}", tenant, query.q(), query.type());

        // 分面在「关键字命中集」上统计（含各类型计数，供前端继续按类型收窄）；类型过滤再作用于结果列表。
        List<AssetSummary> keywordMatched =
                SEED.stream().filter(asset -> matchesKeyword(asset, query.q())).toList();
        List<Facet> facets = facetByType(keywordMatched);

        List<AssetSummary> matched =
                (query.type() == null)
                        ? keywordMatched
                        : keywordMatched.stream()
                                .filter(asset -> asset.type() == query.type())
                                .toList();

        int total = matched.size();
        int from = Math.min((query.page() - 1) * query.pageSize(), total);
        int to = Math.min(from + query.pageSize(), total);
        List<AssetSummary> pageItems = matched.subList(from, to);

        return new MetaSearchResult(pageItems, query.page(), query.pageSize(), total, facets);
    }

    private static boolean matchesKeyword(AssetSummary asset, String q) {
        if (q == null) {
            return true;
        }
        String needle = q.toLowerCase(Locale.ROOT);
        if (asset.name().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        return asset.tags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(needle));
    }

    private static List<Facet> facetByType(List<AssetSummary> assets) {
        Map<AssetType, Long> counts =
                assets.stream()
                        .collect(Collectors.groupingBy(AssetSummary::type, Collectors.counting()));
        List<FacetBucket> buckets =
                counts.entrySet().stream()
                        .sorted(Comparator.comparingInt(entry -> entry.getKey().ordinal()))
                        .map(entry -> new FacetBucket(entry.getKey().wire(), Math.toIntExact(entry.getValue())))
                        .toList();
        return buckets.isEmpty() ? List.of() : List.of(new Facet("type", buckets));
    }
}
