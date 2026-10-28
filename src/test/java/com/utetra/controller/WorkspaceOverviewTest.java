package com.utetra.controller;
import com.utetra.order.service.OperationsService;
import com.utetra.security.UtetraPrincipal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.ui.ExtendedModelMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class WorkspaceOverviewTest {
 @Test void ownerOverviewShowsEveryOwnedStoreAndNoForeignStore(){
  var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:overview"+System.nanoTime()+";DB_CLOSE_DELAY=-1","sa",""));
  jdbc.execute("create table stores(id bigint,name varchar(100),status varchar(30),accepting_orders boolean,owner_user_id bigint)");
  jdbc.update("insert into stores values(1,'Owned active','ACTIVE',true,7),(2,'Owned pending','PENDING_APPROVAL',false,7),(3,'Foreign store','ACTIVE',true,8)");
  var ops=mock(OperationsService.class);when(ops.revenue(any(),any(),any(),any())).thenReturn(Map.of("orders",0,"goods",0,"storeNet",0));
  var controller=new WorkspaceController(new com.utetra.service.WorkspaceService(ops,new com.utetra.repository.WorkspaceRepository(jdbc),Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"),ZoneOffset.UTC)));var model=new ExtendedModelMap();
  assertEquals("workspace/vendor",controller.vendor(new UtetraPrincipal(7L,1L,"ROLE_VENDOR",Instant.MAX),null,null,model));
  var branches=(List<?>)model.get("branches");assertEquals(2,branches.size());
  assertEquals("Owned active",((Map<?,?>)branches.get(0)).get("name"));assertEquals("Owned pending",((Map<?,?>)branches.get(1)).get("name"));
  var empty=new ExtendedModelMap();controller.vendor(new UtetraPrincipal(99L,1L,"ROLE_VENDOR",Instant.MAX),null,null,empty);assertTrue(((List<?>)empty.get("branches")).isEmpty());
 }
}
