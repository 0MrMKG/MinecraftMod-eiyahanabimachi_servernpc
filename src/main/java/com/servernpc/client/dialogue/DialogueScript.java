package com.servernpc.client.dialogue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public final class DialogueScript {
    public static final String END_NODE = "END";

    private final String npcName;
    @Nullable
    private final String portraitFileName;
    private final String startNodeId;
    private final Map<String, DialogueNode> nodes;

    public DialogueScript(
            String npcName,
            @Nullable String portraitFileName,
            String startNodeId,
            Map<String, DialogueNode> nodes
    ) {
        this.npcName = npcName;
        this.portraitFileName = portraitFileName;
        this.startNodeId = startNodeId;

        Map<String, DialogueNode> copy = new LinkedHashMap<>();
        for (Map.Entry<String, DialogueNode> entry : nodes.entrySet()) {
            DialogueNode node = entry.getValue();
            copy.put(
                    entry.getKey(),
                    new DialogueNode(node.id(), node.text(), List.copyOf(node.options()), node.nextNodeId())
            );
        }
        this.nodes = Map.copyOf(copy);
    }

    public String npcName() {
        return this.npcName;
    }

    @Nullable
    public String portraitFileName() {
        return this.portraitFileName;
    }

    public String startNodeId() {
        return this.startNodeId;
    }

    @Nullable
    public DialogueNode node(String id) {
        return this.nodes.get(id);
    }

    public static DialogueScript errorScript(String npcName, String detail) {
        DialogueNode node = new DialogueNode(
                "start",
                "剧本加载失败:\n" + detail,
                List.of(),
                END_NODE
        );
        return new DialogueScript(npcName, null, "start", Map.of("start", node));
    }

    public record DialogueNode(
            String id,
            String text,
            List<DialogueOption> options,
            @Nullable String nextNodeId
    ) {
    }

    public record DialogueOption(String text, String nextNodeId) {
    }
}
