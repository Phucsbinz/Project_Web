package com.utetra.service;

import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.order.service.OperationsService;
import com.utetra.repository.WorkspaceRepository;
import com.utetra.security.UtetraPrincipal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class WorkspaceService {
 private final OperationsService operations;
 private final WorkspaceRepository stores;
 private final Clock clock;
 public WorkspaceService(OperationsService operations,WorkspaceRepository stores,Clock clock){this.operations=operations;this.stores=stores;this.clock=clock;}
 public Map<String,Object> vendorOverview(UtetraPrincipal principal,LocalDate from,LocalDate to){
  var actor=new Actor(principal.userId(),principal.role());
  LocalDate today=LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));
  LocalDate start=from==null?today.minusDays(6):from,end=to==null?today:to;
  var report=operations.revenue(actor,null,start,end);
  var comparison=new ArrayList<Map<String,Object>>();
  for(var branch:stores.ownedStores(principal.userId())){
   var row=new LinkedHashMap<String,Object>(branch);
   row.put("report",operations.revenue(actor,((Number)branch.get("id")).longValue(),start,end));
   comparison.add(row);
  }
  return Map.of("report",report,"branches",comparison);
 }
}
