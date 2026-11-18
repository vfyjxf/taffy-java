# P0-a 行为变更 allowlist

> 依据 Q0=A（接受 bug 修复带来的数值变化）。golden 基线：`e5e767f`（49b7ef0 上冻结）；本表由 6 个 golden 场景的实际 diff 生成。
> 规则：每条变化必须有 old/new/reason/issue/since/removeBy；stale/过期条目同样失败。
> 注：PA-05（abspos 50% 高度）尚未合并，合并后补第 6 行。

| # | 场景 | 路径 | 字段 | old | new | 修复 | 原因 | since | removeBy |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | grid_named_lines_absolute_position | nodes/1 | location.x / location.y / size.width / size.height | 0,0,200,150 | 10,10,100,70 | PA-01 | 绝对定位命名行 a/c 之前未解析、铺满整格 | 1.2.0 | 不删除（bug fix） |
| 2 | grid_named_lines_absolute_position | nodes/2 | location.x / size.width / size.height | 0,200,150 | 50,150,30 | PA-01 | 命名行 b/auto 解析后落在第二列、高度 30 | 1.2.0 | 不删除 |
| 3 | grid_named_lines_absolute_position | nodes/3 | location / size | 0,0,200,150 | 10,10,100,70 | PA-01 | 反向命名行 c/a 解析 | 1.2.0 | 不删除 |
| 4 | grid_named_lines_absolute_position | nodes/0 | contentSize.height | 150 | 80 | PA-01 | 绝对子项不再铺满，content extent 变 80 | 1.2.0 | 不删除 |
| 5 | flex_wrap_reverse_align_self | nodes/1,2 | location.y | 80 | 67 | PA-02 | wrap-reverse 下 start 贴当前行物理起点，不再按 flex-start 翻转 | 1.2.0 | 不删除 |
| 6 | flex_wrap_reverse_align_self | nodes/3 | location.y | 33 | 47 | PA-02 | wrap-reverse 下 end 贴当前行物理终点 | 1.2.0 | 不删除 |
| 7 | flex_wrap_reverse_align_self | nodes/0 | contentSize.height | 100 | 87 | PA-02 | 子项位置变化导致内容高度变化 | 1.2.0 | 不删除 |
| 8 | rtl_root_direction | nodes/0 | location.x | 0 | 95 | PA-03 | RTL root 靠右：300 - 200 - margin.right 5 | 1.2.0 | 不删除 |
| 9 | style_copy_flex_shorthand | nodes/0 | contentSize.width | 20 | 100 | PA-04 | copy() 保留 flex 后子项撑满容器 | 1.2.0 | 不删除 |
| 10 | style_copy_flex_shorthand | nodes/2 | size.width | 0 | 80 | PA-04 | flex:2 的 copy 子项正确 grow 到 80 | 1.2.0 | 不删除 |
| 11 | block_display_none_descendants | nodes/2,3,4 | location/size/contentSize（除 order） | 旧可见值（120x80@30、60x25、60x20、30x15@10,10） | 0 | PA-06 | display:none 后代之前从不布局、残留旧值；performChildLayout 清零并写缓存 | 1.2.0 | 不删除 |
| 12 | block_display_none_descendants | nodes/2,4 | order | 1 | 0 | PA-06 | 隐藏节点按子节点真实顺序重写 order | 1.2.0 | 不删除 |

## 待补

- PA-05：`abspos_content_sized_percentage_height` 的 50% 高度 0 → 50，合并后补行。
