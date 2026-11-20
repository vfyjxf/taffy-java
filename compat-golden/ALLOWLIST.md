# P0-a behavior-change allowlist

> Basis: Q0=A (bug fixes may change layout numbers). Golden baseline: `e5e767f` (frozen at 49b7ef0). This table is generated from the actual diffs of the six golden scenarios.
> Rule: every change needs old/new/reason/issue/since/removeBy; stale or expired entries fail as well.
> Note: PA-05 (abspos percentage height) is not merged yet; row 13 is added when it lands.

| # | Scenario | Path | Field | Old | New | Fix | Reason | Since | RemoveBy |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | grid_named_lines_absolute_position | nodes/1 | location.x / location.y / size.width / size.height | 0, 0, 200, 150 | 10, 10, 100, 70 | PA-01 | Named lines a/c were not resolved for abspos children, so the item filled the whole grid | 1.2.0 | never (bug fix) |
| 2 | grid_named_lines_absolute_position | nodes/2 | location.x / size.width / size.height | 0, 200, 150 | 50, 150, 30 | PA-01 | Named line b/auto now resolves to the second column, 30px tall | 1.2.0 | never |
| 3 | grid_named_lines_absolute_position | nodes/3 | location / size | 0, 0, 200, 150 | 10, 10, 100, 70 | PA-01 | Reversed named lines c/a now resolve | 1.2.0 | never |
| 4 | grid_named_lines_absolute_position | nodes/0 | contentSize.height | 150 | 80 | PA-01 | Abspos children no longer fill the grid; content extent becomes 80 | 1.2.0 | never |
| 5 | flex_wrap_reverse_align_self | nodes/1,2 | location.y | 80 | 67 | PA-02 | Under wrap-reverse, `start` is the physical start of its line instead of a flipped flex-start | 1.2.0 | never |
| 6 | flex_wrap_reverse_align_self | nodes/3 | location.y | 33 | 47 | PA-02 | Under wrap-reverse, `end` is the physical end of its line | 1.2.0 | never |
| 7 | flex_wrap_reverse_align_self | nodes/0 | contentSize.height | 100 | 87 | PA-02 | Item positions change, so the content height changes | 1.2.0 | never |
| 8 | rtl_root_direction | nodes/0 | location.x | 0 | 95 | PA-03 | RTL root is right-aligned: 300 - 200 - margin.right(5) | 1.2.0 | never |
| 9 | style_copy_flex_shorthand | nodes/0 | contentSize.width | 20 | 100 | PA-04 | `copy()` keeps flex, so the child fills the container | 1.2.0 | never |
| 10 | style_copy_flex_shorthand | nodes/2 | size.width | 0 | 80 | PA-04 | The copied flex:2 child grows to 80 | 1.2.0 | never |
| 11 | block_display_none_descendants | nodes/2,3,4 | location / size / contentSize (except order) | old visible values (120x80@30, 60x25, 60x20, 30x15@10,10) | 0 | PA-06 | display:none descendants were never laid out and kept stale values; performChildLayout zeroes them and stores the cache | 1.2.0 | never |
| 12 | block_display_none_descendants | nodes/2,4 | order | 1 | 0 | PA-06 | Hidden nodes get their real child order | 1.2.0 | never |
| 13 | abspos_content_sized_percentage_height | nodes/3 | size.height | 0 | 50 | PA-05 | A content-sized abspos container now runs a definite-size second pass, so height:50% resolves against the final 100px height | 1.2.0 | never |
| 14 | abspos_content_sized_percentage_height | nodes/0,1,1 | contentSize.height / scrollHeight | 100 / 0 | 150 / 50 | PA-05 | The percentage child now contributes 50px to the container and root content extent | 1.2.0 | never |
