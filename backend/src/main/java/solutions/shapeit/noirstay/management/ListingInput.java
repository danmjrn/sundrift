package solutions.shapeit.noirstay.management;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Explicit host-editable fields; ownership, currency and publication approval remain server-owned.
 * @author Daniel Jr Nkulu
 */
public record ListingInput(
    @NotBlank @Size(max=120) String title,
    @NotBlank @Size(max=10000) String description,
    @Pattern(regexp="APARTMENT|VILLA|CABIN|SUITE|HOTEL_ROOM") @NotNull String type,
    @NotBlank @Size(max=80) String destination,
    @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
    @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
    @Min(1) @Max(16) int capacity,@Min(1) @Max(16) int beds,@Min(1) @Max(12) int bathrooms,
    @NotNull @Size(max=12) List<@Pattern(regexp="Wi-Fi|Parking|Pool|Kitchen|Ocean view|Air conditioning|Workspace|Fireplace|Garden|Breakfast") String> amenities,
    @NotNull @DecimalMin("100.00") @DecimalMax("100000.00") BigDecimal nightlyRate,
    @NotNull @DecimalMin("0.00") @DecimalMax("10000.00") BigDecimal cleaningFee,
    @NotBlank @Size(max=3000) String houseRules,
    @NotNull @Pattern(regexp="FLEXIBLE|MODERATE") String cancellationPolicy,boolean submit) { }
