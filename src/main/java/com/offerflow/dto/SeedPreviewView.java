package com.offerflow.dto;

import java.util.List;

public record SeedPreviewView(String packId, String title, int totalCount, List<SeedPreviewEntry> samples) {}
