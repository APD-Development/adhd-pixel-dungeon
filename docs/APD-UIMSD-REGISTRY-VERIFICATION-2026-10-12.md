Confirmed fact-registry/class-initialization deadlock, local fix prepared; not committed or delivered yet.

FactKinds.get/all/fingerprint held the registry monitor while lazily invoking GameFacts initialization. GameFacts static fields register back through synchronized FactKinds.register. A concurrent GameFacts initializer and registry reader could therefore own opposite locks and never complete.

Permanent reproduction uses the actual production FactKinds/GameFacts/FactKind bytecode in an isolated class loader, with a controlled pause at GameFacts' first FactKind.Builder load. It does not substitute a fake GameFacts implementation or add production test hooks. Old source reached the actual all() call, then both initializer/reader failed to finish (baseline assertion failed after5.032s). Initial fixture setup paused FactKind before reflection had resolved its method signatures; that setup failure was corrected before the genuine reproduction.

Fix loads built-in class initialization before entering the registry monitor, then synchronizes map reads/copies/fingerprint traversal. register remains synchronized. The JVM's once-only class initialization replaces the premature builtinsLoaded flag. Keys, fields, event IDs, ordinary registration order, fingerprint formula and raw formats remain unchanged.

Controlled regression passed0.084s. Freshly compiled regression plus existing FactKindsTest, FactEventsTest and FactDatasetsTest:20unique passes0.515s, including the permanent field/key manifest. Physical Android startup race not reproduced; this is a verified JVM lock-order race, not a claim about the owner's hang. Full1258core/21API34/debug checks will be required if combined with the two pending mean/median tests.

Broader UIMSD audit remains incomplete. No gameplay, RNG, scores, player saves or raw logs changed.
