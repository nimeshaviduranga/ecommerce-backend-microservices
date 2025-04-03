package com.ecommerce.authservice.service;

import com.ecommerce.authservice.dto.UserDTO;

import java.util.List;

public interface UserService {

    UserDTO getUserById(Long id);

    UserDTO getUserByUsername(String username);

    List<UserDTO> getAllUsers();

    UserDTO updateUser(Long id, UserDTO userDTO);

    void deleteUser(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}