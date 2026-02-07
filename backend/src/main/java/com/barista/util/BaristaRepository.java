package com.barista.util;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.barista.model.Barista;

@Repository
public interface BaristaRepository extends JpaRepository<Barista, Integer> {
}
