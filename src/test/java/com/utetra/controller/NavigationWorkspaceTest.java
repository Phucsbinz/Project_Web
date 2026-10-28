package com.utetra.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import com.utetra.security.UtetraPrincipal;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;

class NavigationWorkspaceTest {
 JdbcTemplate jdbc;NavigationAdvice advice;UtetraPrincipal manager=new UtetraPrincipal(2L,1L,"ROLE_MANAGER",Instant.MAX);
 @BeforeEach void setup(){jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:nav"+System.nanoTime()+";DB_CLOSE_DELAY=-1","sa",""));jdbc.execute("create table stores(id bigint,name varchar(100))");jdbc.execute("create table store_memberships(id bigint,user_id bigint,store_id bigint,is_active boolean,member_role varchar(30))");jdbc.update("insert into stores values(1,'Cơ sở Thủ Đức'),(2,'Cơ sở Quận 1')");advice=new NavigationAdvice(jdbc,mock(ApplicationContext.class));}
 MockHttpServletRequest request(String store){var req=new MockHttpServletRequest("GET","/manager/operations");req.setServletPath("/manager/operations");if(store!=null)req.setParameter("store",store);return req;}
 @Test void titleUsesOnlyCurrentMembershipsAndExplicitManyBranchChoice(){
  assertEquals("Chưa được phân công",advice.workspaceTitle(manager,request(null)));
  jdbc.update("insert into store_memberships values(1,2,1,true,'MANAGER')");
  assertEquals("Quản lý Cơ sở Thủ Đức",advice.workspaceTitle(manager,request(null)));
  jdbc.update("insert into store_memberships values(2,2,2,true,'MANAGER')");
  assertEquals("Quản lý chi nhánh",advice.workspaceTitle(manager,request(null)));
  assertEquals("Quản lý Cơ sở Quận 1",advice.workspaceTitle(manager,request("2")));
  jdbc.update("update store_memberships set is_active=false where store_id=2");
  assertEquals("Quản lý Cơ sở Thủ Đức",advice.workspaceTitle(manager,request("2")));
 }
 @Test void categoryCapabilityRequiresCurrentExplicitDelegation(){
  jdbc.update("insert into store_memberships values(1,2,1,true,'MANAGER')");
  assertFalse(advice.catalogAdministration(manager,request(null)));
  jdbc.update("update store_memberships set member_role='CATALOG_ADMIN'");
  assertTrue(advice.catalogAdministration(manager,request(null)));
  jdbc.update("update store_memberships set is_active=false");
  assertFalse(advice.catalogAdministration(manager,request(null)));
 }
}
