package com.utetra.repository;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WorkspaceRepository {
 private final JdbcTemplate jdbc;
 public WorkspaceRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public List<Map<String,Object>> ownedStores(long owner){
  return jdbc.query("select id,name,status,accepting_orders from stores where owner_user_id=? order by id",(row,index)->{
   var branch=new LinkedHashMap<String,Object>();branch.put("id",row.getLong("id"));branch.put("name",row.getString("name"));branch.put("status",row.getString("status"));branch.put("acceptingOrders",row.getBoolean("accepting_orders"));return branch;
  },owner);
 }
}
