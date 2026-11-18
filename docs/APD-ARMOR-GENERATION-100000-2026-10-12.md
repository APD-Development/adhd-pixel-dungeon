# TEST79 armor generation: 100,000-seed study

Actual source: a611c841e64a8bca592831d3200a5a65e1735b61. Freshly compiled 1,558 core/engine source files. Normal Warrior context: difficulty3, no challenges, exotic setting0. Seeds202610120000–202610219999. The headless process exited0 after2,885.705460907seconds (48minutes5.7seconds), generating22main-depth floors per seed:2,200,000floors. Isolated preferences/save directories; no player history or save modified.

| Outcome by floor22 | Count | Frequency |
|---|---:|---:|
| No armor above leather in generated heaps, including shops | 0/100000 | 0observed |
| No armor above leather in generated non-shop heaps | 0/100000 | 0observed |
| None available under the restricted geometric check | 1770/100000 | 1.77% |

Heap placements:209205leather,336336mail,370576scale,263864plate. Shops normally supply mail on11,scale16,plate21; the all-heap outcome is guaranteed by those normal stocks in this context. The non-shop result independently showed zero omissions across this cohort. An approximate one-sided95% binomial upper bound for zero non-shop observations is0.003%, assuming representative/independently behaving seeds. It is not a universal guarantee.

Generated is not encountered. Restricted checking flood-fills passable/avoid cells from the entrance and excludes sale and locked/crystal chest loot. It does not model exploration, hidden dangers, health, keys, purchases, decisions or combat and is not a player-encounter probability. Counts cover main-depth heaps, excluding held statue armor,NPC rewards,side branches,future kill drops; adding those sources cannot make a dungeon with already-generated higher armor lack it.

Earlier source comparisons found normal APD armor tier probabilities and shop progression match SPD's fork reference. No loot changes are justified by this cohort. The owner's exact run remains unresolved without its seed/settings/path/history; do not call that reported issue definitively explained or fixed. Test79 confirmation alone does not supply difficulty/challenges/hero/shop visits.

This100000cohort includes the earlier10000range; do not sum them as110000independent runs. The separate500000direct armor draws are item draws,not dungeon sequences. Raw output and its checksum are preserved separately. No API spending, simulator outcome fabrication or recording into player statistics.
