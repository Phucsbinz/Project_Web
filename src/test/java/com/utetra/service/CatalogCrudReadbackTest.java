package com.utetra.service;

import com.utetra.entity.catalog.*;
import com.utetra.repository.catalog.*;
import com.utetra.security.UtetraPrincipal;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogCrudReadbackTest {
    final CatalogGuard guard=mock(CatalogGuard.class);
    final ToppingRepository toppings=mock(ToppingRepository.class);
    final StoreToppingRepository stock=mock(StoreToppingRepository.class);
    final CatalogService service=new CatalogService(null,null,toppings,null,null,stock,null,guard,null,null,null,null);
    final UtetraPrincipal principal=new UtetraPrincipal(1L,1L,"ROLE_MANAGER",Instant.now().plusSeconds(3600));

    @Test void availabilityReadbackIncludesUnavailableAndGloballyInactiveDefinitions() {
        var active=new Topping(); active.setName("Pearls");active.setPrice(BigDecimal.TEN);active.setActive(true);
        var inactive=new Topping(); inactive.setName("Jelly");inactive.setPrice(BigDecimal.ONE);inactive.setActive(false);
        when(toppings.findAll()).thenReturn(List.of(active,inactive));
        when(stock.findByStoreId(7L)).thenReturn(List.of());
        var result=service.storeToppings(principal,7L);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(x->x.available()).containsExactly(false,false);
        assertThat(result).extracting(x->x.active()).containsExactly(true,false);
        verify(guard).store(principal,7L);
    }
    @Test void readbackUsesSavedAvailabilityForTheRequestedStore() {
        var topping=new Topping();topping.setName("Pearls");topping.setPrice(BigDecimal.TEN);topping.setActive(true);
        org.springframework.test.util.ReflectionTestUtils.setField(topping,"id",3L);
        var saved=new StoreTopping();saved.setStoreId(7L);saved.setToppingId(3L);saved.setAvailable(true);
        when(toppings.findAll()).thenReturn(List.of(topping));
        when(stock.findByStoreId(7L)).thenReturn(List.of(saved));
        assertThat(service.storeToppings(principal,7L).getFirst().available()).isTrue();
        saved.setAvailable(false);
        assertThat(service.storeToppings(principal,7L).getFirst().available()).isFalse();
    }
    @Test void readbackRequiresStoreScopeBeforeReadingAvailability() {
        doThrow(new IllegalStateException("forbidden")).when(guard).store(principal,9L);
        assertThatThrownBy(()->service.storeToppings(principal,9L)).hasMessage("forbidden");
        verifyNoInteractions(toppings,stock);
    }
}
