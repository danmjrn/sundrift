package solutions.shapeit.noirstay.catalogue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import solutions.shapeit.noirstay.booking.Pricing;
import solutions.shapeit.noirstay.common.ApiException;
import solutions.shapeit.noirstay.common.Database;

/** Parameterised, paginated listing projections for one independently rentable unit each.
 * @author Daniel Jr Nkulu
 */
@Service
public class CatalogueService {
    private static final String SELECT="""
        SELECT l.*,u.display_name AS host_name,COALESCE(r.rating,0) AS rating,COALESCE(r.reviews,0) AS review_count
        FROM listing l JOIN app_user u ON u.id=l.host_id
        LEFT JOIN (SELECT listing_id,round(avg(rating),2) rating,count(*) reviews FROM review GROUP BY listing_id) r ON r.listing_id=l.id
        """;
    private final Database db;
    private final Pricing pricing;
    public CatalogueService(Database db,Pricing pricing) { this.db=db;this.pricing=pricing; }
    public Map<String,Object> search(String context,String destination,LocalDate in,LocalDate out,int guests,
        BigDecimal min,BigDecimal max,String type,String amenity,BigDecimal rating,String sort,int page) {
        if(page<0 || page>1000 || guests<1 || guests>16 || min.signum()<0 || max.compareTo(min)<0)
            throw new ApiException(400,"INVALID_FILTER","Check your filters.");
        if((in==null)!=(out==null)) throw new ApiException(400,"INVALID_DATES","Select both arrival and departure.");
        if(in!=null) pricing.validate(in,out,guests,16,"Africa/Johannesburg");
        String order=switch(sort) { case "price_asc" -> "l.nightly_rate ASC"; case "price_desc" -> "l.nightly_rate DESC";
            case "rating" -> "COALESCE(r.rating,0) DESC"; case "recommended" -> "COALESCE(r.rating,0) DESC,l.nightly_rate ASC";
            default -> throw new ApiException(400,"INVALID_SORT","Unsupported sort order."); };
        StringBuilder where=new StringBuilder(" WHERE l.context=? AND l.state='PUBLISHED' AND l.destination ILIKE ? AND l.capacity>=? AND l.nightly_rate BETWEEN ? AND ? AND COALESCE(r.rating,0)>=?");
        List<Object> args=new ArrayList<>(List.of(context,"%"+destination+"%",guests,min,max,rating));
        if(!type.isBlank()) { where.append(" AND l.type=?"); args.add(type); }
        if(!amenity.isBlank()) { where.append(" AND l.amenities @> ?::jsonb"); args.add(db.json(List.of(amenity.split(",")))); }
        if(in!=null) { where.append(" AND NOT EXISTS(SELECT 1 FROM inventory i WHERE i.listing_id=l.id AND i.active AND i.check_in<? AND i.check_out>?)"); args.add(out);args.add(in); }
        List<Map<String,Object>> all=db.rows(SELECT+where+" ORDER BY "+order+",l.id LIMIT 13 OFFSET ?",append(args,page*12));
        boolean more=all.size()>12; List<Map<String,Object>> items=new ArrayList<>(all.subList(0,Math.min(12,all.size())));
        if(in!=null) items.forEach(item -> item.put("stayPrice",pricing.calculate((BigDecimal)item.get("nightlyRate"),(BigDecimal)item.get("cleaningFee"),in,out,(String)item.get("cancellationPolicy"))));
        return Map.of("items",items,"page",page,"hasMore",more,"pageSize",12);
    }
    private Object[] append(List<Object> args,Object last) { List<Object> result=new ArrayList<>(args);result.add(last);return result.toArray(); }
    public Map<String,Object> detail(String context,String slug) {
        Map<String,Object> listing=db.one(SELECT+" WHERE l.context=? AND l.slug=? AND l.state='PUBLISHED'",context,slug);
        listing.put("reviews",db.rows("SELECT r.rating,r.body,r.created_at,u.display_name author FROM review r JOIN app_user u ON u.id=r.author_id WHERE r.listing_id=? ORDER BY r.created_at DESC,r.id LIMIT 50",listing.get("id")));
        return listing;
    }
    public Map<String,Object> byId(String context,UUID id) { return db.one(SELECT+" WHERE l.context=? AND l.id=?",context,id); }
    public List<Map<String,Object>> availability(String context,UUID id) {
        byId(context,id); return db.rows("SELECT check_in,check_out,kind FROM inventory WHERE listing_id=? AND active ORDER BY check_in LIMIT 200",id);
    }
}
