# Vehicle catalogue and guided forms

Implemented 2026-10-25, with existing dependency versions unchanged.

The vehicle wizard asks for identity first, then the initial actual odometer and
currency. Optional specifications, purchase information and photo remain editable.
Year, currency, transmission and country use selectors. Make/model selectors search
locally in the loaded choices, accept manual entry, normalize accents and include
common Cyrillic brand aliases. Changing make clears the previous model.

`VehicleCatalogue` uses the official NHTSA vPIC public API:

- `https://vpic.nhtsa.dot.gov/api/vehicles/GetMakesForVehicleType/car?format=json`
- `https://vpic.nhtsa.dot.gov/api/vehicles/GetModelsForMake/{encodedMake}?format=json`
- Documentation: https://vpic.nhtsa.dot.gov/api/

Only the make is included in the model request. Vehicle IDs, journal, VIN, photos,
questions and mileage are not sent. No API key or new network dependency is needed.
NHTSA receives normal connection metadata. This is a convenience catalogue primarily
covering manufacturer submissions for the US market, not a worldwide authoritative
model/trim/engine compatibility database. A small clearly explained offline seed,
24-hour disk cache and manual entry keep setup usable without a connection.
Catalogues load outside the main thread; requests are bounded by time/response size,
previous model requests are cancelled, and remote names are length checked.

Maintenance uses category chips, date/distance/both selection, a native date picker,
quick relative date/distance choices and an explicit repeat switch. Presets are user
convenience values, not manufacturer recommendations. Interval fields are selectors
with custom input. Optional warnings/cost/notes are collapsed. Saves retain the
existing repository validation, exact unit conversions and transactional completion.

Fuel requires volume and total paid. Mileage is explicitly optional, with the latest
recorded reading displayed and an opt-in field prefilled for confirmation. An explicitly selected average L/100 km can produce a separately labelled mileage
estimate; it never becomes an actual odometer reading. Unknown-mileage fuel remains in financial/volume statistics;
measured consumption excludes affected intervals. Editing an old fuel record preserves
its actual reading by default. Room v2 retains nullable actual readings and adds separate estimate metadata with a
v1 migration. Portable backup v2 imports v1 and preserves estimates separately.

UI strings cover English, Russian and Ukrainian. Illustration fallback resources and
exact replacement names are listed in `DriveCare_Image_Briefs_RU.txt` at the repo root.
The AI Garage itself has no header artwork, including its empty state.
