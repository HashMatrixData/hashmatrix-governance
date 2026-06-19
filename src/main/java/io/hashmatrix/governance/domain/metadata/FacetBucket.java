package io.hashmatrix.governance.domain.metadata;

/**
 * 分面桶，对齐契约 {@code governance-metadata-v1} 的 {@code FacetBucket}。
 *
 * @param value 分面取值（如类型 {@code table}）
 * @param count 命中计数
 */
public record FacetBucket(String value, int count) {}
