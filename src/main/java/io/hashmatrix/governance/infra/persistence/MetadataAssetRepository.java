package io.hashmatrix.governance.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 元数据资产持久化（PostgreSQL，schema 由 Flyway 管理）。
 *
 * <p>所有读路径以租户为第一约束（D9）：查询一律带 {@code tenantId}，绝不返回跨租户资产。
 * 供 #28 {@code PostgresMetadataAdapter}（search 真查）与 #29 写端点消费。
 */
public interface MetadataAssetRepository extends JpaRepository<MetadataAssetEntity, UUID> {

    /** 按租户列出全部资产（创建时间倒序），租户隔离边界内的检索基础。 */
    List<MetadataAssetEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    /** 按 id + 租户取单条：确保跨租户不可越权读到他人资产（D9）。 */
    Optional<MetadataAssetEntity> findByIdAndTenantId(UUID id, String tenantId);
}
