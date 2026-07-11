package com.offerflow;

import com.offerflow.dto.CompanyForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidWebsiteUrl() {
        CompanyForm form = new CompanyForm();
        form.setName("Acme");
        form.setWebsiteUrl("not-a-url");

        Set<ConstraintViolation<CompanyForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("官网链接")));
    }

    @Test
    void allowsBlankOptionalUrls() {
        CompanyForm form = new CompanyForm();
        form.setName("Acme");

        assertTrue(validator.validate(form).isEmpty());
    }

    @Test
    void allowsHttpsUrls() {
        CompanyForm form = new CompanyForm();
        form.setName("Acme");
        form.setCareersUrl("https://jobs.example.com");
        form.setReferralUrl("http://refer.example.com/path");

        assertTrue(validator.validate(form).isEmpty());
    }
}
