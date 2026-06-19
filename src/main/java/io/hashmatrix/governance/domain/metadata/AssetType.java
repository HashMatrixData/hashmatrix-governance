package io.hashmatrix.governance.domain.metadata;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/**
 * 资产类型枚举，对齐契约 {@code governance-metadata-v1} 的 {@code AssetType}。
 *
 * <p>线上表示为小写串（{@code table}/{@code view}/…）：{@link #wire()} 经 {@link JsonValue} 序列化，
 * {@link #fromWire(String)} 大小写不敏感解析查询参数。契约枚举不可随意增删——增值须先改契约。
 */
public enum AssetType {
    TABLE,
    VIEW,
    COLUMN,
    DATASET,
    API;

    /** 契约线上表示（小写）。 */
    @JsonValue
    public String wire() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * 由查询参数解析资产类型（大小写不敏感，容错）。
     *
     * <p>{@code null}/空白/**非契约枚举值**一律返回 {@code null}（等价「不按类型过滤」）——
     * 与 {@link SearchQuery#of} 对分页越界的收敛一致，使 {@code /api/meta/search} 全程 happy-path
     * （契约仅声明 200，不引入未声明的 4xx）。
     *
     * @param raw 原始串
     * @return 对应类型，或 {@code null}（未指定 / 不可识别 → 不过滤）
     */
    public static AssetType fromWire(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
