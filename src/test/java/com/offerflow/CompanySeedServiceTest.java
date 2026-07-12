package com.offerflow;

import com.offerflow.dto.SeedImportResult;
import com.offerflow.repository.CompanyRepository;
import com.offerflow.service.CompanySeedService;
import com.offerflow.service.UnknownCompanySeedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CompanySeedServiceTest {

    @Autowired
    private CompanySeedService seedService;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void seedFileContainsEighteenCompanies() {
        assertEquals(18, seedService.countSeedEntries(CompanySeedService.JAVA_BACKEND_INTERNET));
    }

    @Test
    void importSeedOnEmptyDatabaseImportsAll() {
        SeedImportResult result = seedService.importSeed(CompanySeedService.JAVA_BACKEND_INTERNET);

        assertEquals(18, result.total());
        assertEquals(18, result.imported());
        assertEquals(0, result.skipped());
    }

    @Test
    void importSeedSecondTimeSkipsAll() {
        seedService.importSeed(CompanySeedService.JAVA_BACKEND_INTERNET);

        SeedImportResult second = seedService.importSeed(CompanySeedService.JAVA_BACKEND_INTERNET);

        assertEquals(18, second.total());
        assertEquals(0, second.imported());
        assertEquals(18, second.skipped());
    }

    @Test
    void financeTechSeedContainsTenCompanies() {
        assertEquals(10, seedService.countSeedEntries(CompanySeedService.FINANCE_TECH));
    }

    @Test
    void foreignTechSeedImportsIdempotently() {
        SeedImportResult first = seedService.importSeed(CompanySeedService.FOREIGN_TECH);
        assertEquals(10, first.imported());

        SeedImportResult second = seedService.importSeed(CompanySeedService.FOREIGN_TECH);
        assertEquals(0, second.imported());
        assertEquals(10, second.skipped());
    }

    @Test
    void listAvailableSeedsIncludesAllPacks() {
        assertEquals(3, seedService.listAvailableSeeds().size());
    }

    @Test
    void rejectsUnknownSeedId() {
        assertThrows(UnknownCompanySeedException.class, () -> seedService.importSeed("unknown-pack"));
    }

    @Test
    void previewSeedReturnsFirstFiveSamples() {
        var preview = seedService.previewSeed(CompanySeedService.FINANCE_TECH);

        assertEquals(CompanySeedService.FINANCE_TECH, preview.packId());
        assertEquals(10, preview.totalCount());
        assertEquals(5, preview.samples().size());
        assertTrue(preview.samples().stream().anyMatch(entry -> entry.name().contains("蚂蚁")));
    }

    @Test
    void importedCompaniesHaveCareersUrl() {
        seedService.importSeed(CompanySeedService.JAVA_BACKEND_INTERNET);

        var bytedance = companyRepository.findByNameIgnoreCase("字节跳动");
        assertTrue(bytedance.isPresent());
        assertEquals("https://jobs.bytedance.com", bytedance.get().getCareersUrl());
    }
}
