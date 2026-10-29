package com.utetra.order;

import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.order.repository.CommerceReferenceRepository;
import com.utetra.order.service.OperationsService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryRateReadbackTest {
    @Test void rateReadbackIdentifiesExactPairEvenWhenDisplayNamesRepeatAndKeepsScope() {
        var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:rates_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
        db.execute("create table carriers(id bigint primary key,name varchar(100))");
        db.execute("create table stores(id bigint primary key,name varchar(100))");
        db.execute("create table store_delivery_areas(id bigint primary key,store_id bigint,name varchar(100))");
        db.execute("create table carrier_area_rates(id bigint primary key,carrier_id bigint,delivery_area_id bigint,additional_fee decimal(12,2),is_active boolean)");
        db.execute("insert into carriers values(1,'Same'),(2,'Same')");
        db.execute("insert into stores values(7,'Shop'),(8,'Shop')");
        db.execute("insert into store_delivery_areas values(11,7,'Area'),(12,8,'Area')");
        db.execute("insert into carrier_area_rates values(21,1,11,5000,true),(22,2,12,6000,false)");
        var actor=new Actor(3,"ROLE_MANAGER");
        var refs=spy(new CommerceReferenceRepository(db));
        doNothing().when(refs).requireCurrent(actor,false);
        doReturn(true).when(refs).operates(actor,7L);
        doReturn(false).when(refs).operates(actor,8L);
        var service=new OperationsService(null,refs,null,null,null);
        var rows=service.deliveryRates(actor);
        assertThat(rows).hasSize(1);
        assertThat(((Number)rows.getFirst().get("id")).longValue()).isEqualTo(21L);
        assertThat(((Number)rows.getFirst().get("carrier_id")).longValue()).isEqualTo(1L);
        assertThat(((Number)rows.getFirst().get("area_id")).longValue()).isEqualTo(11L);
        assertThat(rows.getFirst().get("is_active")).isEqualTo(true);
    }
}
