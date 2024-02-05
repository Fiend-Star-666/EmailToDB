package com.emailtodb.emailtodb.repositories;

import com.emailtodb.emailtodb.entities.Guidance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuidanceRepository extends JpaRepository<Guidance, Long> {

}
