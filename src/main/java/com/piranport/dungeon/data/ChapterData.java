package com.piranport.dungeon.data;

import java.util.List;

/**
 * Parsed chapter configuration from JSON.
 */
public record ChapterData(
        String chapterId,
        String displayName,
        int sortOrder,
        List<String> stages
) {}
