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
    void rejectsUnknownSeedId() {
        assertThrows(UnknownCompanySeedException.class, () -> seedService.importSeed("unknown-pack"));
    }

    @Test
    void importedCompaniesHaveCareersUrl() {
        seedService.importSeed(CompanySeedService.JAVA_BACKEND_INTERNET);

        var bytedance = companyRepository.findByNameIgnoreCase("字节跳动");
        assertTrue(bytedance.isPresent());
        assertEquals("https://jobs.bytedance.com", bytedance.get().getCareersUrl());
    }
}
