package com.utetra.identity;
import static com.utetra.identity.IdentityRepository.time;
import com.utetra.security.UtetraPrincipal;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Append-only contract. Enum-only summaries prevent accidental secret/raw-body logging. */
@Service
public class AuditService {
    public enum Action { MANAGE_USER,ASSIGN_MANAGER,HIDE_COMMUNITY,ORDER_STATE_CHANGED,REFUND_RECORDED,STORE_CHANGED,PROMOTION_CHANGED }
    public enum Target { USERS,STORES,REVIEWS,PRODUCT_COMMENTS,ORDERS,PROMOTIONS }
    private final IdentityRepository db; private final AccountService accounts; private final Clock clock;
    public AuditService(IdentityRepository db,AccountService accounts,Clock clock) { this.db=db; this.accounts=accounts; this.clock=clock; }
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(UtetraPrincipal actor,Action action,Target target,long entityId) {
        accounts.require(actor,false);
        if(entityId<=0 || action==null || target==null) throw IdentityException.invalid("Thông tin audit không hợp lệ.");
        db.update("insert into audit_logs(actor_id,action,entity_type,entity_id,summary,created_at) values(?,?,?,?,?,?)",actor.userId(),action.name(),target.name().toLowerCase(java.util.Locale.ROOT),entityId,action.name()+" / "+target.name(),time(clock.instant()));
    }
}
