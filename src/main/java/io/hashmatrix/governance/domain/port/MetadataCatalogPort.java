package io.hashmatrix.governance.domain.port;

import io.hashmatrix.governance.domain.metadata.MetaSearchResult;
import io.hashmatrix.governance.domain.metadata.SearchQuery;

/**
 * 元数据目录出站端口（六边形架构）：屏蔽元数据来源（M1 mock，后续 PG(JSONB)+ES）。
 *
 * <p>{@code app} 层经本端口检索，不耦合具体来源。M1 由 {@code infra} 的 mock 适配器实现；
 * 真实落库（PG/ES，typedef-driven）见 #12，届时新增 Adapter 即可，{@code api}/{@code app} 不动。
 */
public interface MetadataCatalogPort {

    /**
     * 在当前租户隔离边界内检索元数据。
     *
     * @param query 检索条件
     * @return 分页 + 分面结果
     */
    MetaSearchResult search(SearchQuery query);
}
