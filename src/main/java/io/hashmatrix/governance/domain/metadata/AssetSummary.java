package io.hashmatrix.governance.domain.metadata;

import java.util.List;

/**
 * 资产检索摘要，对齐契约 {@code governance-metadata-v1} 的 {@code AssetSummary}。
 *
 * @param id    资产标识（脱敏占位，如 {@code asset-1001}）
 * @param name  资产名（虚构表名，如 {@code orders}）
 * @param type  资产类型
 * @param owner 归属（团队/责任人占位，如 {@code data-team}）
 * @param tags  标签集合，非空（{@code null} 归一化为空列表）
 */
public record AssetSummary(String id, String name, AssetType type, String owner, List<String> tags) {

    public AssetSummary {
        tags = (tags == null) ? List.of() : List.copyOf(tags);
    }
}
