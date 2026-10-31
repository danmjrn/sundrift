package solutions.shapeit.sundrift.common;

/** A safe business failure carrying an explicit HTTP status and stable code.
 * @author Daniel Jr Nkulu
 */
public class ApiException extends RuntimeException {
    public final int status;
    public final String code;
    public ApiException(int status, String code, String message) { super(message); this.status = status; this.code = code; }
    public static ApiException missing() { return new ApiException(404, "NOT_FOUND", "This resource is unavailable."); }
    public static ApiException forbidden() { return new ApiException(403, "FORBIDDEN", "You do not have access to this resource."); }
    public static ApiException conflict(String message) { return new ApiException(409, "CONFLICT", message); }
}
