package com.utetra.order.service;

import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.order.repository.CommerceReferenceRepository;
import com.utetra.security.UtetraPrincipal;
import com.utetra.service.StoreAccessPolicy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
public class CommerceStoreAccessPolicy implements StoreAccessPolicy {
  private final CommerceReferenceRepository refs;

  public CommerceStoreAccessPolicy(CommerceReferenceRepository refs) {
    this.refs = refs;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canOperate(UtetraPrincipal p, Long store) {
    Actor actor = new Actor(p.userId(), p.role());
    refs.requireCurrent(actor, false);
    return refs.operates(actor, store);
  }
}
