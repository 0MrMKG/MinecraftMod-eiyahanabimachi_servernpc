package com.servernpc.client.dialogue;

import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.entity.ReimuGoodNpcEntity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class DialogueScriptLoader {
    private static final String DIALOGUE_DIR = "dialogues";
    private static final String SCRIPT_DIR = "scripts";
    private static final String ILLUSTRATION_DIR = "illustration";
    private static final String README_FILE = "README.txt";

    private DialogueScriptLoader() {
    }

    public static LoadedDialogue loadForNpc(ReimuGoodNpcEntity npc) {
        String npcEntityId = resolveNpcEntityId(npc);
        String fallbackNpcName = sanitizeName(npc.getDisplayName().getString());
        Path rootDir = resolveDialogueRoot();
        Path scriptsDir = rootDir.resolve(SCRIPT_DIR);
        Path illustrationDir = rootDir.resolve(ILLUSTRATION_DIR);
        Path scriptPath = scriptsDir.resolve(npcEntityId + ".txt");

        try {
            Files.createDirectories(scriptsDir);
            Files.createDirectories(illustrationDir);
            ensureReadme(rootDir);
            if (Files.notExists(scriptPath)) {
                writeTemplateScript(scriptPath, npcEntityId, fallbackNpcName);
            }
            DialogueScript script = parse(scriptPath, fallbackNpcName, npcEntityId);
            return new LoadedDialogue(script, scriptPath, illustrationDir);
        } catch (IOException ex) {
            eiyahanabimachiservernpc.LOGGER.warn("Failed to prepare dialogue files for {}", npcEntityId, ex);
            DialogueScript error = DialogueScript.errorScript(fallbackNpcName, "无法创建剧本目录或模板");
            return new LoadedDialogue(error, scriptPath, illustrationDir);
        }
    }

    private static Path resolveDialogueRoot() {
        Path localRoot = Path.of(DIALOGUE_DIR).toAbsolutePath().normalize();
        try {
            Files.createDirectories(localRoot);
            return localRoot;
        } catch (IOException ignored) {
        }

        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        return gameDir
                .resolve("config")
                .resolve(eiyahanabimachiservernpc.MODID)
                .resolve(DIALOGUE_DIR)
                .toAbsolutePath()
                .normalize();
    }

    private static void ensureReadme(Path rootDir) throws IOException {
        Path readmePath = rootDir.resolve(README_FILE);
        if (Files.exists(readmePath)) {
            return;
        }
        String readme = """
                对话系统脚本目录说明

                目录结构：
                - scripts/: 每个 NPC 一个 txt 剧本，文件名为实体 ID（例如 hinanawi_tenshi.txt）
                - illustration/: 立绘图片目录（png）

                剧本格式：
                [npc]
                name=显示名称
                portrait=立绘文件名.png
                start=start

                [node start]
                text=第一句话。支持\\n换行。
                option=选项文本->下一个节点ID
                option=离开->END

                [node next_id]
                text=下一段文字
                next=END

                规则：
                - option 和 next 的目标写 END 表示关闭对话。
                - option 可写多行；next 只在无选项时生效。
                - 修改后重新右键 NPC 即可读取最新内容。
                """;
        Files.writeString(readmePath, readme, StandardCharsets.UTF_8);
    }

    private static void writeTemplateScript(Path scriptPath, String npcEntityId, String fallbackNpcName) throws IOException {
        String template = """
                # 自动生成模板：首次右键此 NPC 时创建。
                [npc]
                name=%s
                portrait=%s.png
                start=start

                [node start]
                text=你好，我是%s。\\n这是一个可编辑的对话模板。
                option=你是谁？->intro
                option=下次再聊->END

                [node intro]
                text=你可以在 scripts/%s.txt 中自由改写剧情分支。
                next=END
                """.formatted(fallbackNpcName, npcEntityId, fallbackNpcName, npcEntityId);
        Files.writeString(scriptPath, template, StandardCharsets.UTF_8);
    }

    private static DialogueScript parse(Path scriptPath, String fallbackNpcName, String npcEntityId) {
        List<String> lines;
        try {
            lines = Files.readAllLines(scriptPath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            eiyahanabimachiservernpc.LOGGER.warn("Failed to read dialogue script {}", scriptPath, ex);
            return DialogueScript.errorScript(fallbackNpcName, "读取剧本失败");
        }

        String npcName = fallbackNpcName;
        String portrait = npcEntityId + ".png";
        String startNode = "start";
        Map<String, NodeDraft> drafts = new LinkedHashMap<>();

        @Nullable String activeNodeId = null;
        boolean inNpcSection = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                String section = line.substring(1, line.length() - 1).trim();
                if (section.equalsIgnoreCase("npc")) {
                    inNpcSection = true;
                    activeNodeId = null;
                    continue;
                }

                String lower = section.toLowerCase(Locale.ROOT);
                if (lower.startsWith("node ")) {
                    String nodeId = section.substring(5).trim();
                    if (!nodeId.isEmpty()) {
                        drafts.computeIfAbsent(nodeId, NodeDraft::new);
                        activeNodeId = nodeId;
                        inNpcSection = false;
                    }
                    continue;
                }

                inNpcSection = false;
                activeNodeId = null;
                continue;
            }

            int eq = line.indexOf('=');
            if (eq <= 0 || eq == line.length() - 1) {
                continue;
            }
            String key = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(eq + 1).trim();

            if (inNpcSection) {
                switch (key) {
                    case "name" -> npcName = value.isEmpty() ? fallbackNpcName : value;
                    case "portrait" -> portrait = value;
                    case "start" -> startNode = value.isEmpty() ? startNode : value;
                    default -> {
                    }
                }
                continue;
            }

            if (activeNodeId == null) {
                continue;
            }
            NodeDraft node = drafts.get(activeNodeId);
            if (node == null) {
                continue;
            }

            switch (key) {
                case "text" -> node.text = unescapeText(value);
                case "next" -> node.nextNodeId = value;
                case "option" -> {
                    int arrow = value.indexOf("->");
                    if (arrow > 0 && arrow < value.length() - 2) {
                        String optionText = value.substring(0, arrow).trim();
                        String target = value.substring(arrow + 2).trim();
                        if (!optionText.isEmpty() && !target.isEmpty()) {
                            node.options.add(new DialogueScript.DialogueOption(optionText, target));
                        }
                    }
                }
                default -> {
                }
            }
        }

        if (drafts.isEmpty()) {
            return DialogueScript.errorScript(npcName, "剧本中未找到任何 [node ...] 节点");
        }

        Map<String, DialogueScript.DialogueNode> nodes = new LinkedHashMap<>();
        for (NodeDraft draft : drafts.values()) {
            String text = draft.text == null ? "" : draft.text;
            nodes.put(
                    draft.id,
                    new DialogueScript.DialogueNode(
                            draft.id,
                            text,
                            List.copyOf(draft.options),
                            draft.nextNodeId
                    )
            );
        }

        if (!nodes.containsKey(startNode)) {
            startNode = new LinkedList<>(nodes.keySet()).getFirst();
        }
        return new DialogueScript(npcName, portrait, startNode, nodes);
    }

    private static String resolveNpcEntityId(ReimuGoodNpcEntity npc) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(npc.getType());
        if (key == null) {
            return "unknown_npc";
        }
        return key.getPath();
    }

    private static String sanitizeName(String name) {
        return name.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static String unescapeText(String raw) {
        return raw.replace("\\n", "\n");
    }

    public record LoadedDialogue(DialogueScript script, Path scriptPath, Path illustrationDirectory) {
    }

    private static final class NodeDraft {
        private final String id;
        @Nullable
        private String text;
        @Nullable
        private String nextNodeId;
        private final List<DialogueScript.DialogueOption> options = new ArrayList<>();

        private NodeDraft(String id) {
            this.id = id;
        }
    }
}
