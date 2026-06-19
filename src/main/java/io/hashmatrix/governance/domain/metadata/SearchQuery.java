package io.hashmatrix.governance.domain.metadata;

/**
 * 元数据检索查询条件（领域值对象），对齐契约 {@code /api/meta/search} 的查询参数。
 *
 * <p>分页越界经 {@link #of} 收敛到合法区间（契约定义 {@code page>=1}、{@code pageSize∈[1,200]} 及默认值）；
 * 直接构造则由紧凑构造器守护不变量。{@code q} 空白归一化为 {@code null}（等价「不按关键字过滤」）。
 *
 * @param q        检索关键字，{@code null} 表示不过滤
 * @param type     资产类型过滤，{@code null} 表示不过滤
 * @param page     页码（1 基）
 * @param pageSize 页大小
 */
public record SearchQuery(String q, AssetType type, int page, int pageSize) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 200;

    public SearchQuery {
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be in [1, " + MAX_PAGE_SIZE + "]");
        }
        q = (q == null || q.isBlank()) ? null : q.trim();
    }

    /**
     * 由查询参数构造，缺省/越界收敛到合法区间（mock 容错，不抛 4xx）。
     *
     * @param q        关键字（可空）
     * @param type     类型过滤（可空）
     * @param page     页码（可空→默认 1；&lt;1 收敛为 1）
     * @param pageSize 页大小（可空→默认 20；越界收敛到 [1,200]）
     */
    public static SearchQuery of(String q, AssetType type, Integer page, Integer pageSize) {
        int p = (page == null) ? DEFAULT_PAGE : Math.max(1, page);
        int ps =
                (pageSize == null)
                        ? DEFAULT_PAGE_SIZE
                        : Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
        return new SearchQuery(q, type, p, ps);
    }
}
