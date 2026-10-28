package com.s4.neighborhood.client;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class MetaSegmentationClientTest {
    private final MetaSegmentationClient client = new MetaSegmentationClient(
            "https://api.meta.ai/v1", "test-only", "sam-3.1", JsonMapper.builder().build());

    @Test
    void assemblesRealResponseChunksAndConfirmsCompleted() throws Exception {
        String chunks = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"<0f>\"}\n\n"
                + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"0<|box;x1=1;y1=2;x2=3;y2=4;w=12;h=12|>\"}\n\n"
                + "data: {\"type\":\"response.content_part.done\",\"part\":{\"type\":\"output_text\",\"text\":\"<0f>0<|box;x1=1;y1=2;x2=3;y2=4;w=12;h=12|>\"}}\n\n"
                + "data: {\"type\":\"response.completed\",\"response\":{\"id\":\"resp-test\"}}\n\n"
                + "data: [DONE]\n";
        var result = client.readEvents(new BufferedReader(new StringReader(chunks)));
        assertEquals("sam-3.1", result.model());
        assertEquals("resp-test", result.responseId());
        assertTrue(result.rawOutput().contains("<0f>"));
    }

    @Test
    void emptyCompletedResultIsValidZeroMatches() throws Exception {
        String events = "data: {\"type\":\"response.output_text.done\",\"text\":\"\"}\n\n"
                + "data: {\"type\":\"response.completed\",\"response\":{\"id\":\"resp-empty\"}}\n";
        assertEquals("", client.readEvents(new BufferedReader(new StringReader(events))).rawOutput());
    }

    @Test
    void incompleteStreamNeverBecomesSuccessful() {
        String incomplete = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"partial\"}\n";
        assertThrows(IllegalStateException.class,
                () -> client.readEvents(new BufferedReader(new StringReader(incomplete))));
    }

    @Test
    void mismatchedFinalizerIsRejected() {
        String text = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"first\"}\n"
                + "data: {\"type\":\"response.output_text.done\",\"text\":\"different\"}\n"
                + "data: {\"type\":\"response.completed\",\"response\":{\"id\":\"resp-x\"}}\n";
        assertThrows(IllegalStateException.class,
                () -> client.readEvents(new BufferedReader(new StringReader(text))));
    }
}
