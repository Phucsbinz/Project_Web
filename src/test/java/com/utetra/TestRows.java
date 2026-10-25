package com.utetra;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

/** Complete required columns in isolated integration fixtures against the merged schema. */
public final class TestRows {
 private TestRows() {}
 public static long insert(JdbcTemplate jdbc,String table,Map<String,Object> supplied) {
  var values=new LinkedHashMap<String,Object>(supplied);
  jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) c -> {
   try(var r=c.getMetaData().getColumns(null,null,table.toUpperCase(Locale.ROOT),null)) {
    while(r.next()) {
     String name=r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT);
     if(values.containsKey(name)||"YES".equals(r.getString("IS_AUTOINCREMENT"))||r.getInt("NULLABLE")!=DatabaseMetaData.columnNoNulls||r.getString("COLUMN_DEF")!=null)continue;
     Object value=switch(r.getInt("DATA_TYPE")) {
      case Types.BOOLEAN,Types.BIT -> false;
      case Types.BIGINT,Types.INTEGER,Types.SMALLINT,Types.TINYINT,Types.NUMERIC,Types.DECIMAL,Types.FLOAT,Types.DOUBLE -> 0;
      case Types.TIMESTAMP,Types.TIMESTAMP_WITH_TIMEZONE -> Timestamp.from(Instant.now());
      case Types.TIME,Types.TIME_WITH_TIMEZONE -> Time.valueOf("08:00:00");
      default -> ("t"+UUID.randomUUID().toString().replace("-","")).substring(0,Math.min(33,r.getInt("COLUMN_SIZE")));
     };
     values.put(name,value);
    }
   }return null;
  });
  if(values.containsKey("id")) {new SimpleJdbcInsert(jdbc).withTableName(table).usingColumns(values.keySet().toArray(String[]::new)).execute(values);return ((Number)values.get("id")).longValue();}
  return new SimpleJdbcInsert(jdbc).withTableName(table).usingColumns(values.keySet().toArray(String[]::new)).usingGeneratedKeyColumns("id").executeAndReturnKey(values).longValue();
 }
}
