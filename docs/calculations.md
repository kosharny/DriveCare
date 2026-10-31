# Calculations, dates and integrity

## Exact values and units

Money is a signed 64-bit integer of nonnegative minor units with an ISO 4217 currency
code on every paid record. `BigDecimal` calculates and rounds to the currency's scale
with HALF_UP on confirmation; binary floating point is used only to draw chart pixels.
Volume and odometer use canonical decimal strings, liters and kilometers. Exact
conversion constants: 1 mile = 1.609344 km; 1 US gallon = 3.785411784 liters.
Original entry units are retained. Drafts retain their exact canonical values while
the user changes display units; the canonical value is replaced only when the
corresponding numeric input is explicitly edited. Changing display preferences never reinterprets
stored money, distance or volume. Changing a unit inside a draft converts its values.

Fuel mileage is optional. Without an actual reading, the record still contributes
spending and purchased volume, but does not update mileage or provide a measured
full-to-full interval. The owner can edit it later to add the actual reading. The
last recorded reading is only a reference until explicitly confirmed. The owner can explicitly opt into the separate fuel-based estimate described below.
Portable archives preserve both actual and estimated fields without mixing them.

Volume and total paid are normal fuel inputs. Total paid is authoritative. Editing
unit price recalculates total at the currency scale; editing volume/total recalculates
the displayed unit price. There is one field-owner update per action, no reactive loop.
Totals and average prices remain separated by currency and fuel stream; no FX exists.

## Full-to-full consumption

For consecutive full fills A and B, volume includes fills strictly after A through B.
A's purchased volume is excluded. Partial fills between A and B are included. Eligible
consumption = sum(eligible liters) / sum(eligible kilometers) × 100. It is not the
arithmetic average of interval percentages. A single full tank gives no measurement.

The specification's 10,000 km full 40 L, 10,200 km partial 12 L, 10,600 km full 30 L
sequence yields 600 km / 42 L and 7.0 L/100 km. Purchased volume across those three
records is separately 82 L; it is not the consumed interval volume.

Intervals with missing fills, non-increasing mileage, unknown mileage, duplicate fuel
timestamps or incompatible streams are excluded. A later unambiguous full fill starts
a fresh baseline. LPG/dual-fuel and mixed streams expose unavailable consumption with
an explanation, while retaining their spending. The date of the ENDING full fill
attributes complete intervals to periods. This is explicitly not exact fuel use within
a calendar month. PHEV fuel records exclude charging energy. Fuel-based mileage estimates never enter measured consumption, paid spend/km or
maintenance status. No health score or projected repair cost is calculated.

## Statistics

Fuel amount sums confirmed paid records. Purchased volume sums selected fuel records.
Weighted unit price = compatible currency/fuel group's total spend / purchased volume.
45 L × 60 UAH + 30 L × 62 UAH = 4,560 UAH, 75 L and 60.8 UAH/L before rounding.
Fuel, service and other subtotals are separate. Service completion creates one journal
record, not a duplicate expense. Estimated plan costs never enter paid totals.
Advanced comparisons use the preceding equal-length date range and label its dates;
calendar presets themselves use actual month/year boundaries.

## Dates and odometer

Fuel/expense times are instants (epoch milliseconds). Service and due dates are ISO
LocalDate values. The first launch persists a reporting ZoneId in DataStore, independent
of later device locale changes. Date filters are half-open [start day, next day after
end), using ZoneId day boundaries to handle DST. Service records use their explicit
calendar date. Local entry times use java.time ZoneId resolution: overlaps select the
earlier offset and gaps shift forward by the transition duration. Currency and UI language are independent. Report time zone is explicit
and editable; changing it deliberately changes grouping, not stored instants.

Every journal record that contains odometer is a dated reading with its own stable ID,
origin kind and vehicle relationship. Initial and manual odometer entries are journal
records as well. Latest reading is chosen by timestamp, not insertion order. Saves
validate the candidate against chronological neighbors, excluding the edited record;
same-time readings must agree. Legitimate lower historical insertion is allowed.
Conflicts require correction of the date/mileage or explicit editing of the mistaken
journal reading. Financial record and associated reading are the SAME atomic record.

## Maintenance

A plan is separate from its completion records. Both date and distance may be set;
either threshold is sufficient. Date-only and distance-only plans remain valid; no
date is extrapolated from mileage. Warning days/distance are user editable. A stale
reading is labeled, and distance status means status at that recorded reading.

Completion and recalculation use one Room transaction. Latest actual service date +
calendar months yields the next date; actual odometer + distance interval yields the
next distance. Calendar months use LocalDate.plusMonths, including end-of-month
clamping; no fixed 30-day month exists. Missing dimensions stay unknown. One-time
plans become completed. Edits/deletions recalculate from remaining latest completion,
falling back to user-confirmed initial thresholds. Deleting a plan preserves service
history and its cost through a nullable foreign key.

WorkManager periodically reconciles active unpaused plans and also reconciles after
record/plan edits. The stable plan notification tag and persisted last-notified local
day suppress repeats. App launches/upgrades reconcile periodic scheduling. Time zone
is read at execution time; timezone changes never create fake mileage. Device power
management can delay delivery. No exact-alarm or location permission is requested.

## Portable archive v2 (imports v1 and v2)

ZIP contains `manifest.json` (version, reportingZone, vehicles, plans, records,
conversations, attachment metadata) and referenced `attachments/<generated-name>`.
No credentials, billing state, App Check tokens or SDK caches are included. Restore
rejects unexpected/duplicate/path-traversing entries, unknown versions, invalid IDs,
foreign keys, monetary/decimal values and odometer chronology. Limits: 5,001 ZIP
entries, 15 MiB per entry, 100 MiB expanded total, 100,000 records, 1,000 vehicles,
10,000 plans and 5,000 conversations/attachments. Export validates the same schema/size limits before writing. Short entry fields
are limited to 250 characters, notes to 10,000 and direct numeric input to 24
characters, so ordinary saved input remains portable. Files are staged and renamed;
replacement is one database transaction. Replaced attachment files are removed
after commit. Acknowledging successful restore closes stale forms and returns to
Overview. Failed validation retains live database data.
CSV uses UTF-8 BOM, escaped fields and formula-injection protection. Canonical units
are explicitly labeled in CSV. PDF uses Android PdfDocument and the system Unicode
font, line wrapping and page breaks; no external PDF/font SDK is required.

## Optional fuel-based mileage estimate

Owners may enter an assumed average consumption in **L/100 km**, independently of
display distance/volume units. Canonical estimated distance is `liters × 100 / assumed
L per 100 km`, calculated with BigDecimal at six decimal places. The accepted average
is 0.1–1,000 L/100 km; actual odometer and estimate mode are mutually exclusive for a
fuel record. The latest entered assumption is suggested next time, but estimation
requires explicit opt-in for each new fill. It is never inferred by AI.

The overview adds eligible fuel distances after the latest actual dated reading to
that actual baseline and labels the result **approximate mileage after using the
fuel**. It also shows the original verified reading/date. It assumes the purchased
fuel is used; remaining tank volume, traffic, loads and changing consumption are
unknown. The UI recommends periodic odometer checks. A new actual reading resets
the baseline; editing/deleting a fuel record recomputes the estimate. Unknown-average
fills and missing-fill flags produce a partial-estimate explanation.

An optional anchor record ID handles the ordering of a verification and a new fill
within the same timestamp minute: an older estimated fill cannot reappear just
because another fill was entered during that minute. Editing an existing estimate
without changing its time preserves its anchor. The anchor is provenance, not a
foreign key; deleting a reading does not delete its fuel history.

Room v1 → v2 adds nullable `estimatedConsumptionLPer100Km` and
`estimateAnchorRecordId` columns with an explicit non-destructive migration. Existing
rows retain NULL values. Backup export uses portable version 2; v1 imports default
these fields to NULL. Validation rejects estimate metadata on non-fuel records,
invalid averages or simultaneous actual readings. CSV adds separate assumed-average
and estimated-distance columns; it never writes estimates into the actual odometer
column. PDF labels the assumption when present. Old app versions reject v2 archives
rather than silently stripping the new fields.
