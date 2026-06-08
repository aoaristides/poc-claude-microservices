package com.example.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockItemJpaRepository extends JpaRepository<StockItemJpaEntity, String> {

    /**
     * Busca vários itens de uma vez para evitar N+1.
     * Usa FOR UPDATE para travar as linhas durante a transação de reserva (lock pessimista por default).
     */
    @Query("select s from StockItemJpaEntity s where s.sku in :skus")
    List<StockItemJpaEntity> findBySkuIn(@Param("skus") List<String> skus);
}
