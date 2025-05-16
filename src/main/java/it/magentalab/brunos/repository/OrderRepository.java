package it.magentalab.brunos.repository;

import it.magentalab.brunos.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByNameAndArticle(String name, String article);
}