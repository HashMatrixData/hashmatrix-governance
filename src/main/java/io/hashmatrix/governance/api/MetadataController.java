package io.hashmatrix.governance.api;

import io.hashmatrix.governance.app.MetadataQueryService;
import io.hashmatrix.governance.domain.metadata.AssetType;
import io.hashmatrix.governance.domain.metadata.MetaSearchResult;
import io.hashmatrix.governance.domain.metadata.SearchQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 元数据供数 API（governance 是契约 {@code governance-metadata-v1} 的 producer）。
 *
 * <p>返回值直接是契约 DTO（{@link MetaSearchResult}），**不套** {@code ApiResponse} 信封——
 * 契约把 200 建模为裸 payload、错误另用 {@code Error} schema；内部自检端点（{@code probe}）仍用统一信封。
 * 结果按租户隔离（租户上下文由网关注入 {@code X-Tenant-*}，经 {@code starter-tenant} 解析）。
 *
 * <p>M1：{@code /api/meta/search} 返回脱敏 mock（#4）；{@code detail/lineage/perm/open} 见 #12/#15/#16。
 */
@RestController
@RequestMapping("/api/meta")
public class MetadataController {

    private final MetadataQueryService metadataQueryService;

    public MetadataController(MetadataQueryService metadataQueryService) {
        this.metadataQueryService = metadataQueryService;
    }

    /**
     * 元数据检索 / 分面（契约 {@code searchMetadata}）。
     *
     * @param q        检索关键字（可选）
     * @param type     资产类型过滤（可选，小写串如 {@code table}；空/不可识别 → 不过滤）
     * @param page     页码（可选，默认 1）
     * @param pageSize 页大小（可选，默认 20，上限 200）
     * @return 检索结果（含分面），裸契约结构
     */
    @GetMapping("/search")
    public MetaSearchResult search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        // 入参一律容错（type 不可识别 → 不过滤、分页越界 → 收敛）：契约仅声明 200，不引入未声明 4xx。
        SearchQuery query = SearchQuery.of(q, AssetType.fromWire(type), page, pageSize);
        return metadataQueryService.search(query);
    }
}
