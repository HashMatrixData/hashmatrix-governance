package io.hashmatrix.governance.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.hashmatrix.governance.domain.metadata.MetaSearchResult;
import io.hashmatrix.governance.domain.metadata.SearchQuery;
import io.hashmatrix.governance.domain.port.MetadataCatalogPort;
import io.hashmatrix.starter.audit.AuditEvent;
import io.hashmatrix.starter.audit.AuditRecorder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataQueryServiceTest {

    @Mock private MetadataCatalogPort catalog;
    @Mock private AuditRecorder auditRecorder;

    @Test
    void delegatesToCatalogAndRecordsAudit() {
        SearchQuery query = SearchQuery.of("orders", null, 1, 20);
        MetaSearchResult expected = new MetaSearchResult(List.of(), 1, 20, 0, List.of());
        when(catalog.search(query)).thenReturn(expected);
        MetadataQueryService service = new MetadataQueryService(catalog, auditRecorder);

        MetaSearchResult result = service.search(query);

        assertThat(result).isSameAs(expected);
        verify(catalog).search(query);
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditRecorder).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("META_SEARCH");
        assertThat(captor.getValue().outcome()).isEqualTo(AuditEvent.Outcome.SUCCESS);
    }
}
