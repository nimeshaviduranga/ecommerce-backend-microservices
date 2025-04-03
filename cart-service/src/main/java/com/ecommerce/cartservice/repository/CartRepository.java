package com.ecommerce.cartservice.repository;

import com.ecommerce.cartservice.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {

    /**
     * Find a cart by user ID
     *
     * @param userId The ID of the user
     * @return Optional containing the cart if found
     */
    Optional<Cart> findByUserId(Long userId);
}