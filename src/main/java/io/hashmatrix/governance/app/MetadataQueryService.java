package io.hashmatrix.governance.app;

import io.hashmatrix.governance.domain.metadata.MetaSearchResult;
import io.hashmatrix.governance.domain.metadata.SearchQuery;
import io.hashmatrix.governance.domain.port.MetadataCatalogPort;
import io.hashmatrix.starter.audit.AuditEvent;
import io.hashmatrix.starter.audit.AuditRecorder;
import org.springframework.stereotype.Service;

/**
 * 元数据检索应用服务：在当前租户上下文内经 {@link MetadataCatalogPort} 检索，并复用
 * {@code starter-audit} 记一次审计。审计事件由 {@link AuditEvent#of} 自动加盖当前租户（{@code starter-tenant}）。
 */
@Service
public class MetadataQueryService {

    private final MetadataCatalogPort catalog;
    private final AuditRecorder auditRecorder;

    public MetadataQueryService(MetadataCatalogPort catalog, AuditRecorder auditRecorder) {
        this.catalog = catalog;
        this.auditRecorder = auditRecorder;
    }

    /**
     * 检索元数据并记录审计。
     *
     * @param query 检索条件
     * @return 分页 + 分面结果
     */
    public MetaSearchResult search(SearchQuery query) {
        MetaSearchResult result = catalog.search(query);
        auditRecorder.record(
                AuditEvent.of(
                        // post-M1：取真实调用方（网关 JWT subject / 服务账号），现以 system 占位
                        "system",
                        "META_SEARCH",
                        "meta/search",
                        AuditEvent.Outcome.SUCCESS,
                        "hits=" + result.total()));
        return result;
    }
}
