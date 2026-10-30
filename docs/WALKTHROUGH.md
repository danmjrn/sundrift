# Five-minute demo

Start the documented Compose stack and open http://localhost:3000. Use dates relative to
today; fixtures use their initialization date, and repeated startup does not duplicate them.

1. Search **Cape Town**, the next suitable future weekend, **2 guests**. If a seeded block
   occupies the weekend, choose the following weekend. Filter **Parking**, set a nightly
   budget (for example R1,000–R3,500) and apply. Search state is in the URL.
2. Open a stay, inspect the gallery and availability calendar, and select arrival/departure.
   Review the nightly rate and complete stay price, including cleaning, service and VAT.
3. Choose **Sign in to reserve**, then **Demo guest**. Entry creates a private catalogue;
   the return link retains the slug and dates. Continue to reservation to obtain a hold.
4. Accept the displayed rules/policy, select **Success** and confirm the **simulated** payment.
   The trip reference, agreed price and confirmation are saved in PostgreSQL. Refresh.
5. Open **My trips**, then the saved trip. In **Portfolio demo**, choose **Demo host** to
   switch within the same context. Host studio shows this reservation and persisted metrics.
6. Switch back to **Demo guest**, open the reservation, preview the policy-based refund,
   then confirm cancellation. The trip remains in Cancelled; searching those dates again
   shows the released inventory. A last-minute policy can legitimately return R0.
7. As **Demo host**, create or edit a draft, upload a JPEG/PNG and submit for publication.
   As **Demo admin**, approve/unpublish the listing, inspect synthetic users/bookings/payment
   attempts and open Audit. Existing trip snapshots remain after unpublication.
8. Open **Our approach** for the engineering overview. In the repository, show
   `twoIndependentGuestsCannotObtainSameDates`, `bookingAndHostBlockRaceUsesOneInventoryInvariant`
   and the PostgreSQL exclusion constraint in migration V1.

Payment scenarios:

| Scenario | Persisted result | Inventory |
| --- | --- | --- |
| Success | CONFIRMED, one SUCCESS attempt | Retained |
| Decline | FAILED, one DECLINE attempt | Released |
| Temporary failure | HOLD, one RETRY attempt | Retained until original expiry |

After a retry choose Success; it uses a new attempt key. A network retry of the same request
with the same key replays the prior response. Never enter card details: there are no card
fields, no payment-provider credentials and no outgoing booking email integration.

Completed fixture stays have explicitly fictional reviews. A confirmed stay without an
existing review becomes reviewable on its checkout date. The synthetic fixtures are there
to make the dashboards meaningful, not evidence of commercial users or bookings.
