对话系统脚本目录说明

目录结构：
- scripts/: 每个 NPC 一个 txt 剧本，文件名为实体 ID（例如 hinanawi_tenshi.txt）
- illustration/: 立绘图片目录（png）

剧本格式：
[npc]
name=中文显示名称
portrait=立绘文件名.png
start=start

[node start]
text=第一句话。支持\n换行。
# 进入节点时执行函数，多个用 ; 分隔
# 当前支持：set_mainhand_item|minecraft:diamond_sword|1
functions=set_mainhand_item|minecraft:air|1
option=选项文本->下一个节点ID
option=离开->END

[node next_id]
text=下一段文字
next=END

规则：
- option 和 next 的目标写 END 表示关闭对话。
- option 可写多行；next 只在无选项时生效。
- functions 在进入节点时触发并由服务端执行。
- 修改后重新右键 NPC 即可读取最新内容。
