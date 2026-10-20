package com.utetra.identity;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** SQL access for identity-owned schema. All mutations are called inside service transactions. */
@Repository @Transactional
public class IdentityRepository {
    private final EntityManager em;
    public IdentityRepository(EntityManager em) { this.em=em; }
    public int update(String sql,Object... args) {
        var query=em.createNativeQuery(sql); bind(query,args); int changed=query.executeUpdate();
        // Never detach entities owned by the calling order/catalog transaction.
        return changed;
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    public List<Map<String,Object>> list(String sql,Object... args) {
        var query=em.createNativeQuery(sql).unwrap(NativeQuery.class); bind(query,args);
        query.setTupleTransformer((tuple,aliases) -> {
            Map<String,Object> row=new LinkedHashMap<>();
            for(int i=0;i<aliases.length;i++) row.put(aliases[i].toLowerCase(Locale.ROOT),tuple[i]);
            return row;
        });
        return query.getResultList();
    }
    public Map<String,Object> one(String sql,Object... args) {
        var rows=list(sql,args); if(rows.isEmpty()) throw IdentityException.missing(); return rows.getFirst();
    }
    public Optional<Map<String,Object>> optional(String sql,Object... args) {
        var rows=list(sql,args); return rows.stream().findFirst();
    }
    public long count(String sql,Object... args) { var query=em.createNativeQuery(sql); bind(query,args); return ((Number)query.getSingleResult()).longValue(); }
    public long insert(String sql,Object... args) {
        // Generated keys are read from the same Hibernate connection and SQL transaction.
        return em.unwrap(Session.class).doReturningWork(connection -> {
            try(var statement=connection.prepareStatement(sql,new String[]{"id"})) {
                for(int i=0;i<args.length;i++) statement.setObject(i+1,args[i]);
                statement.executeUpdate();
                try(var keys=statement.getGeneratedKeys()) { if(!keys.next()) throw new java.sql.SQLException("Missing generated key"); return keys.getLong(1); }
            }
        });
    }
    private void bind(jakarta.persistence.Query query,Object[] args) {
        for(int i=0;i<args.length;i++) {
            Object value=args[i] instanceof Timestamp timestamp ? timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC) : args[i];
            query.setParameter(i+1,value);
        }
    }
    public Map<String,Object> user(long id,boolean lock) {
        if(lock) one("select id from users where id=? for update",id);
        return one("select u.*, r.name as role_name from users u join roles r on r.id=u.role_id where u.id=?",id);
    }
    public static long number(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    public static String string(Map<String,Object> row,String key) { return Objects.toString(row.get(key),""); }
    public static boolean flag(Map<String,Object> row,String key) {
        var v=row.get(key); return Boolean.TRUE.equals(v) || v instanceof Number n && n.intValue()!=0;
    }
    public static Instant instant(Map<String,Object> row,String key) {
        var v=row.get(key); if(v==null) return null;
        if(v instanceof Timestamp t) return t.toInstant();
        if(v instanceof java.time.LocalDateTime t) return t.toInstant(java.time.ZoneOffset.UTC);
        if(v instanceof java.time.OffsetDateTime t) return t.toInstant();
        return (Instant)v;
    }
    public static Timestamp time(Instant value) { return Timestamp.from(value); }
}
