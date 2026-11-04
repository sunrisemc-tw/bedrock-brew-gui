# Verification — BedrockBrewGUI

Date: 2026-11-04 (Asia/Taipei). No commit, push, deployment, server launch,
or restart was performed.

## Baseline and reproduced defects

- Read all original Java, tests, `plugin.yml`, `pom.xml`, `.gitignore`, and the
  complete task before editing.
- `mvn -B test`: exit 1, 7 tests, 1 failure (category expectation).
- `mvn -B clean test`: exit 1, same 7 tests/1 failure. All original Java actually
  compiled against Paper 1.21.11 and the installed Floodgate/Cumulus artifacts;
  stale output was not the cause.
- Added and ran a regression against the original water classifier:
  `mvn -B test`: exit 1, 8 tests/2 failures. A WATER-based potion with custom
  effects incorrectly counted as water; the other failure was taxonomy.
- Primary-source recipe/fuel/API findings are in `RESEARCH.md`. Mojang's
  official server jar was inspected, not executed.

## Pass 1 — catalog, costs, inventory safety, and initial callback review

Review/fixes:

- Catalog uses compiled `PotionType` and `Material` constants. Tests compare its
  41 entries with the entire target enum minus WATER/AWKWARD/MUNDANE/THICK/LUCK.
- Audited weakness's wart-free path, corruption order, turtle shell, and all
  four 1.21 effect ingredients. Paths match the official vanilla registrations.
- Corrected category expectations by asserting exact membership: BUFF 4,
  UTILITY 4, COMBAT 4, NEGATIVE 7 (19 base effects). No invented fifth effect.
- Preserved one ingredient/modifier per selected 1–3-bottle batch; added
  20-operation fuel accounting with persistent per-player unused fuel and a
  separate strength ingredient; lingering returns one glass bottle.
- Replaced direct slot mutation with detached removal/placement, followed by an
  exact live snapshot recheck and one storage-content write.
- Water classification rejects custom effects and compares exact vanilla water
  metadata, protecting renamed, dyed, and plugin-tagged bottles.
- Capacity is checked after removals, including returned bottles. If all outputs
  cannot fit, fail with no consumption instead of risking cancelled world drops.
- Added per-player sessions and single-use page tickets claimed before
  scheduling; quit/refusal/retirement cleanup cannot erase a newer session.
- All response handling is queued to the player's entity scheduler. Retired
  callbacks only modify concurrent session bookkeeping.
- Initial new inventory tests exposed server-registry-dependent `Material.isAir`
  in the headless harness and a nested Mockito stubbing error. Replaced the
  unnecessary registry lookup with explicit AIR constants and corrected the
  fixture; no production Bukkit stubs were introduced.

Commands/results:

- `mvn -B test` after core rewrite: exit 0, BUILD SUCCESS, 14 tests,
  0 failures/errors/skips.
- `mvn -B test` with new inventory/session/text tests: exit 1, 27 tests,
  7 fixture/registry errors; corrected as described above.
- `mvn -B test` after fixes: exit 0, BUILD SUCCESS, **27 tests,
  0 failures, 0 errors, 0 skipped** (19:10:19 +08:00).

Remaining work at this pass: real Cumulus response/navigation tests, detailed
listener/foreign-thread/lifecycle review, clean packaging and jar inspection.
No live Canvas/Folia/Bedrock integration test has been performed.

## Pass 2 — Forms/API, thread affinity, and lifecycle

Review/fixes:

- Fed encoded Cumulus responses through `FormDefinitions` instead of calling
  handlers directly. This verified dropdown/step-slider indices, page back
  buttons, paginated negative effects, exact missing-material text, and
  confirmation replay behavior.
- Foreign-thread callbacks schedule only through `player.getScheduler()`.
  Inventory, PDC, sound, logging, messages, and form navigation happen in the
  scheduled callback.
- Added tests for duplicate callback races, duplicate entry, quit invalidation,
  scheduler refusal, retired callbacks, plugin-disable scheduling races,
  failed form sends, permission/online rechecks, malformed responses, and
  Bedrock/main-hand brewing-stand interception. Java players and non-right
  clicks remain untouched.
- Caught and fixed a real late-shutdown exception (`IllegalPluginAccessException`)
  and made the normal entry point tolerate the same shutdown race.
- Ran Google Java Format 1.28.0 across every Java source file.

Command/result:

- `mvn -B test`: exit 0, BUILD SUCCESS, **41 tests,
  0 failures, 0 errors, 0 skipped** (19:14:55 +08:00).
- After formatting, `mvn -B test`: exit 0, BUILD SUCCESS, **41 tests,
  0 failures, 0 errors, 0 skipped** (19:17:40 +08:00).

Remaining work at this pass: final full source review, clean package, and jar
contents/dependency inspection. No live Canvas/Folia/Bedrock integration test
has been performed.

## Pass 3 — final full review, clean build, and artifact inspection

Review/fixes:

- Reviewed every production Java file after formatting for recipe correctness,
  Folia affinity, callback replay, inventory races, exact water classification,
  fuel/remainder economics, permissions, Bedrock-only interception, and
  lifecycle cleanup.
- Static audit found no legacy Bukkit scheduler calls, dynamic
  `PotionType.valueOf`/`Material.valueOf` calls, direct world-drop shortcuts,
  or obsolete full-inventory checks.
- `git diff --check` passed with no whitespace errors.
- Ran `mvn -B clean package`: exit 0, BUILD SUCCESS, all **41 tests passed**
  (19:33:03 +08:00).
- Inspected `target/bedrock-brew-gui-1.0.0.jar`: `plugin.yml` is present;
  only `tw/sunrisemc/bedrockbrew/**` classes and Maven metadata are included;
  no Floodgate, Cumulus, JUnit, Mockito, or test classes are shaded.
- Final `mvn -B test`: exit 0, BUILD SUCCESS, **41 tests,
  0 failures, 0 errors, 0 skipped**. Static audit also passed.
- After the final translation cleanup, a second final `mvn -B test` again
  exited 0 with **41 tests, 0 failures, 0 errors, 0 skipped**; the final jar
  audit and `git diff --check` also passed.

Known limitation: no live server was launched, and no production Canvas/Folia/
Bedrock integration test was possible or attempted. Inventory overflow is
rejected before consumption when the complete output (including the lingering
glass-bottle remainder) cannot fit; this is intentional to guarantee no item
loss if an item-spawn event is cancelled.
