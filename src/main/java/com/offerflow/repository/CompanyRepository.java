package com.offerflow.repository;

import com.offerflow.model.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Company> findAllByOrderByNameAsc();

    List<Company> findByIndustryOrderByNameAsc(String industry);

    List<Company> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<Company> findByNameContainingIgnoreCaseAndIndustryOrderByNameAsc(String name, String industry);
}
