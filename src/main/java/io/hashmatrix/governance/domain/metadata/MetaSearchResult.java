package io.hashmatrix.governance.domain.metadata;

import java.util.List;

/**
 * 元数据检索结果（分页 + 分面），对齐契约 {@code governance-metadata-v1} 的 {@code MetaSearchResult}。
 *
 * <p>作为契约 200 响应的**裸 payload** 直接序列化（不套平台 {@code ApiResponse} 信封）——
 * 契约把成功响应建模为该结构、错误另用 {@code Error} schema，消费方按契约解析。
 *
 * @param items    本页资产摘要，非空
 * @param page     当前页（1 基）
 * @param pageSize 页大小
 * @param total    命中总数（分页前）
 * @param facets   分面聚合，非空
 */
public record MetaSearchResult(
        List<AssetSummary> items, int page, int pageSize, int total, List<Facet> facets) {

    public MetaSearchResult {
        items = (items == null) ? List.of() : List.copyOf(items);
        facets = (facets == null) ? List.of() : List.copyOf(facets);
    }
}
