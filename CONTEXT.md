# Worldrunner

Worldrunner is an independent team running game: runners log real on-foot distance, and that distance moves their teams along a virtual route and up or down leagues each season. It is inspired by kmspiel but has its own accounts and data.

## Players and identity

**Runner**:
A person playing the game, who logs Runs and belongs to Teams.
_Avoid_: User, Player, Member (as a noun for the person)

**Account**:
The login identity a Runner signs in with; distinct from the Runner's role in the game. Deleting an Account deletes its Runs, but distance already credited in closed Weeks stays with the Team as anonymous distance.
_Avoid_: User, Profile

**Display name**:
The name a Runner chooses to be shown to other Runners. Real names are never shown.
_Avoid_: Username, nickname

## Distance

**Run**:
One on-foot activity (running or walking, including treadmill) logged by a Runner with its distance, typed in or imported. Other movement, such as cycling, is not a Run. Runs are self-reported and trusted, within the Run limits.
_Avoid_: Entry, DistanceEntry, Activity, Workout

**Imported run**:
A Run read from another app's recording of an on-foot session (Garmin Connect, Strava, and others, through Health Connect) instead of being typed in. It follows the same Run limits and Week close as any Run, and each recording is imported at most once.
_Avoid_: Synced activity, sync (reserve "sync" for sending Runs to the server)

**Run limits**:
The maximum distance allowed for a single Run and for one Runner's Runs on one day. A Run beyond either limit is rejected.

**Weekly total**:
The sum of a Runner's Runs in one Week; always derived, never entered directly.
_Avoid_: Weekly entry, weekly km

## Time

**Season**:
A roughly half-year span, made of whole Weeks, shared by every Team and League. The first-half Season contains the Weeks whose Thursday falls in January–June; the second-half Season contains the Weeks whose Thursday falls in July–December.
_Avoid_: Round, period

**Week**:
An ISO week (Monday–Sunday) within a Season. A Run belongs to the Week containing the Runner's local date of that Run, in the device's timezone when it was logged. A Run may move between Weeks only while both are before Week close.

**Week close**:
The moment a Week's results become final: Tuesday 23:59 UTC after the Week ends. Before it, Runs in that Week can be added, edited, or deleted; after it, they are locked.
_Avoid_: Deadline, cutoff (reserve "cutoff" for the Joining cutoff)

**Joining cutoff**:
The last point in a Season at which a Runner may start a new Membership.

## Teams and competition

**Team**:
A group of Runners whose combined Runs advance it along the Route and in its League. A Team persists across Seasons and keeps its League position. It has a maximum number of Runners (the Team size cap).

**Captain**:
The one Runner who manages a Team's roster and Invite code; initially its creator. Captaincy can be transferred to a current member, and it passes to the earliest-joined member if the Captain has no Membership at Season start.
_Avoid_: Owner, admin, leader

**Invite code**:
A shareable code or link that is the only way for a Runner to start a Membership in a Team.

**Inactive team**:
A Team with no Memberships at the start of a Season. It is removed from its League for that Season.

**Membership**:
A Runner's participation in one Team for one Season. Memberships do not carry over between Seasons. A Membership lasts until the Season ends, and it only counts Runs from Weeks on or after the Week it started. A Runner may hold a limited number of Memberships per Season, and each Team receives the full distance of every eligible Run. A Captain may end a Membership only before the Joining cutoff; distance already credited stays with the Team.
_Avoid_: Member (for the relationship), Enrollment

**League**:
A group of Teams, fixed at Season start, ranked against each other by total distance within a Season.
_Avoid_: Division, group

**Tier**:
One level of the League pyramid: one League at the top, with each lower Tier holding more Leagues. New Teams enter the lowest Tier.
_Avoid_: Level, rank

**Promotion** / **Relegation**:
A Team moving up or down one Tier for the next Season, based on its final Standing.
_Avoid_: Playoff (none exists)

**Season rollover**:
The transition between Seasons: Promotion and Relegation are applied, Inactive teams are removed, gaps in each Tier are filled top-down from the Tier below, and the lowest Tier is regrouped into Leagues, adding a Tier when needed.

**Standing**:
A Team's rank and total distance within its League at a point in the Season. Ties are broken first by the number of Runners with a nonzero Weekly total in the Season, then by Team age (older ranks higher).

**Route**:
The one fixed, virtual loop around the world that every Team follows: from Lisbon eastwards and back, defined in [`docs/route.md`](docs/route.md).
_Avoid_: Track, course

**Route progress**:
A visual representation of a Team's total distance as a position along the Route. It does not affect scoring.
_Avoid_: Score, checkpoint progress
