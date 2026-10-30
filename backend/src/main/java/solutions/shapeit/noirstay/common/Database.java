package solutions.shapeit.noirstay.common;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Bounded SQL projections avoid entity exposure and catalogue N+1 queries.
 * @author Daniel Jr Nkulu
 */
@Component
public class Database {
    public final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public Database(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc=jdbc; this.mapper=mapper.rebuild().enable(tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build(); }
    public String json(Object value) {
        try { return mapper.writeValueAsString(value); } catch(JacksonException e) { throw new IllegalStateException(e); }
    }
    public Object parse(String value) {
        try { return mapper.readValue(value,Object.class); } catch(JacksonException e) { throw new IllegalStateException(e); }
    }
    public List<Map<String,Object>> rows(String sql,Object... args) {
        List<Map<String,Object>> result=new ArrayList<>();
        for(Map<String,Object> raw:jdbc.queryForList(sql,args)) {
            Map<String,Object> row=new LinkedHashMap<>();
            raw.forEach((key,value) -> {
                StringBuilder name=new StringBuilder(); boolean upper=false;
                for(char ch:key.toCharArray()) { if(ch=='_') upper=true; else { name.append(upper?Character.toUpperCase(ch):ch); upper=false; } }
                Object normalized=value;
                if(value != null && value.getClass().getName().equals("org.postgresql.util.PGobject")) normalized=parse(value.toString());
                if(value instanceof java.sql.Date date) normalized=date.toLocalDate();
                if(value instanceof Timestamp timestamp) normalized=timestamp.toInstant();
                row.put(name.toString(),normalized);
            }); result.add(row);
        } return result;
    }
    public Map<String,Object> one(String sql,Object... args) {
        List<Map<String,Object>> rows=rows(sql,args); if(rows.isEmpty()) throw ApiException.missing(); return rows.getFirst();
    }
}
