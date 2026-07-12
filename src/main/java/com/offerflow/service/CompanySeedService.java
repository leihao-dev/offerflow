package com.offerflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerflow.dto.CompanyForm;
import com.offerflow.dto.CompanySeedEntry;
import com.offerflow.dto.SeedImportResult;
import com.offerflow.dto.SeedPackInfo;
import com.offerflow.dto.SeedPreviewEntry;
import com.offerflow.dto.SeedPreviewView;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanySeedService {

    public static final String JAVA_BACKEND_INTERNET = "java-backend-internet";
    public static final String FINANCE_TECH = "finance-tech";
    public static final String FOREIGN_TECH = "foreign-tech";

    private static final Map<String, String> SEED_RESOURCES = Map.of(
            JAVA_BACKEND_INTERNET, "seeds/java-backend-internet.json",
            FINANCE_TECH, "seeds/finance-tech.json",
            FOREIGN_TECH, "seeds/foreign-tech.json");

    private static final Map<String, String> SEED_TITLES = Map.of(
            JAVA_BACKEND_INTERNET, "Java 后端 · 互联网",
            FINANCE_TECH, "金融 / 金融科技",
            FOREIGN_TECH, "外企科技");

    private static final List<String> SEED_ORDER =
            List.of(JAVA_BACKEND_INTERNET, FINANCE_TECH, FOREIGN_TECH);

    private static final int PREVIEW_SAMPLE_SIZE = 5;

    private final CompanyService companyService;
    private final ObjectMapper objectMapper;

    public CompanySeedService(CompanyService companyService, ObjectMapper objectMapper) {
        this.companyService = companyService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public int countSeedEntries(String seedId) {
        return loadEntries(resolveSeedPath(seedId)).size();
    }

    @Transactional(readOnly = true)
    public List<SeedPackInfo> listAvailableSeeds() {
        return SEED_ORDER.stream()
                .map(id -> new SeedPackInfo(id, SEED_TITLES.get(id), countSeedEntries(id)))
                .toList();
    }

    public SeedImportResult importSeed(String seedId) {
        List<CompanySeedEntry> entries = loadEntries(resolveSeedPath(seedId));
        int imported = 0;
        int skipped = 0;

        for (CompanySeedEntry entry : entries) {
            if (companyService.existsByName(entry.name())) {
                skipped++;
                continue;
            }
            companyService.create(toForm(entry));
            imported++;
        }

        return new SeedImportResult(imported, skipped, entries.size());
    }

    @Transactional(readOnly = true)
    public SeedPreviewView previewSeed(String seedId) {
        List<CompanySeedEntry> entries = loadEntries(resolveSeedPath(seedId));
        String title = SEED_TITLES.getOrDefault(seedId, seedId);
        List<SeedPreviewEntry> samples = entries.stream()
                .limit(PREVIEW_SAMPLE_SIZE)
                .map(entry -> new SeedPreviewEntry(entry.name(), entry.industry()))
                .toList();
        return new SeedPreviewView(seedId, title, entries.size(), samples);
    }

    private String resolveSeedPath(String seedId) {
        String resourcePath = SEED_RESOURCES.get(seedId);
        if (resourcePath == null) {
            throw new UnknownCompanySeedException(seedId);
        }
        return resourcePath;
    }

    private List<CompanySeedEntry> loadEntries(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new IllegalStateException("Seed resource missing: " + resourcePath);
        }
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<List<CompanySeedEntry>>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read seed resource: " + resourcePath, ex);
        }
    }

    private CompanyForm toForm(CompanySeedEntry entry) {
        CompanyForm form = new CompanyForm();
        form.setName(entry.name());
        form.setIndustry(entry.industry());
        form.setWebsiteUrl(entry.websiteUrl());
        form.setCareersUrl(entry.careersUrl());
        form.setReferralNotes(entry.referralNotes());
        return form;
    }
}
