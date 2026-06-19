package io.hashmatrix.governance.infra;

import static org.assertj.core.api.Assertions.assertThat;

import io.hashmatrix.governance.domain.metadata.AssetSummary;
import io.hashmatrix.governance.domain.metadata.AssetType;
import io.hashmatrix.governance.domain.metadata.Facet;
import io.hashmatrix.governance.domain.metadata.MetaSearchResult;
import io.hashmatrix.governance.domain.metadata.SearchQuery;
import org.junit.jupiter.api.Test;

class MockMetadataCatalogAdapterTest {

    private final MockMetadataCatalogAdapter adapter = new MockMetadataCatalogAdapter();

    @Test
    void returnsAllSeedWhenNoFilter() {
        MetaSearchResult result = adapter.search(SearchQuery.of(null, null, 1, 50));

        assertThat(result.total()).isEqualTo(7);
        assertThat(result.items()).hasSize(7);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(50);
    }

    @Test
    void filtersByKeywordAcrossNameAndTags() {
        MetaSearchResult byName = adapter.search(SearchQuery.of("order", null, 1, 20));
        assertThat(byName.items())
                .extracting(AssetSummary::name)
                .containsExactlyInAnyOrder("orders", "order_items", "orders_api");

        MetaSearchResult byTag = adapter.search(SearchQuery.of("crm", null, 1, 20));
        assertThat(byTag.items())
                .extracting(AssetSummary::name)
                .containsExactlyInAnyOrder("customers", "customer_360");
    }

    @Test
    void keywordMatchIsCaseInsensitive() {
        MetaSearchResult result = adapter.search(SearchQuery.of("ORDERS", null, 1, 20));

        assertThat(result.items()).extracting(AssetSummary::name).contains("orders", "orders_api");
    }

    @Test
    void filtersByType() {
        MetaSearchResult tables = adapter.search(SearchQuery.of(null, AssetType.TABLE, 1, 20));

        assertThat(tables.total()).isEqualTo(3);
        assertThat(tables.items()).allMatch(asset -> asset.type() == AssetType.TABLE);
    }

    @Test
    void paginatesWithDisjointPages() {
        MetaSearchResult page1 = adapter.search(SearchQuery.of(null, null, 1, 3));
        MetaSearchResult page2 = adapter.search(SearchQuery.of(null, null, 2, 3));

        assertThat(page1.total()).isEqualTo(7);
        assertThat(page1.items()).hasSize(3);
        assertThat(page2.items()).hasSize(3);
        assertThat(page1.items()).doesNotContainAnyElementsOf(page2.items());
    }

    @Test
    void pageBeyondRangeIsEmptyButKeepsTotal() {
        MetaSearchResult result = adapter.search(SearchQuery.of(null, null, 99, 20));

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isEqualTo(7);
    }

    @Test
    void facetByTypeCountsOverKeywordSetNotTypeFilter() {
        // 关键字 "order" 命中：orders(table)、order_items(table)、orders_api(api)；再按 table 过滤剩 2 条，
        // 但分面统计在关键字集上算（供前端继续按类型收窄），故应同时含 table 与 api 桶。
        MetaSearchResult result = adapter.search(SearchQuery.of("order", AssetType.TABLE, 1, 20));

        assertThat(result.items()).hasSize(2);
        Facet typeFacet = result.facets().get(0);
        assertThat(typeFacet.field()).isEqualTo("type");
        assertThat(typeFacet.buckets())
                .extracting(bucket -> bucket.value())
                .containsExactlyInAnyOrder("table", "api");
    }

    @Test
    void emptyResultHasNoFacets() {
        MetaSearchResult result = adapter.search(SearchQuery.of("nonexistent-xyz", null, 1, 20));

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isZero();
        assertThat(result.facets()).isEmpty();
    }
}
