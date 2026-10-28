package com.utetra.controller;

import com.utetra.security.UtetraPrincipal;
import com.utetra.service.WorkspaceService;
import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class WorkspaceController {
 private final WorkspaceService workspace;
 public WorkspaceController(WorkspaceService workspace){this.workspace=workspace;}
 @GetMapping("/vendor/overview")
 public String vendor(@AuthenticationPrincipal UtetraPrincipal principal,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,Model model){
  model.addAllAttributes(workspace.vendorOverview(principal,from,to));
  return "workspace/vendor";
 }
 @GetMapping("/manager/operations") public String manager(){return "workspace/manager";}
 @GetMapping("/admin/assignments") public String assignments(){return "workspace/assignments";}
}
