package io.hashmatrix.governance.domain.metadata;

import java.util.List;

/**
 * 分面聚合，对齐契约 {@code governance-metadata-v1} 的 {@code Facet}。
 *
 * @param field   分面字段（如 {@code type}）
 * @param buckets 各取值的计数桶，非空（{@code null} 归一化为空列表）
 */
public record Facet(String field, List<FacetBucket> buckets) {

    public Facet {
        buckets = (buckets == null) ? List.of() : List.copyOf(buckets);
    }
}
