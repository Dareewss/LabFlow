package com.labflow.dao;

import com.labflow.model.Role;
import com.labflow.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTest {
    private UserDAO userDAO;

    @BeforeEach
    public void setUp() {
        DatabaseInitializer.initializeDatabase();
        userDAO = new UserDAO();
    }

    @Test
    public void testAuthenticateValidUser() {
        Optional<User> user = userDAO.authenticate("admin", "admin123");
        assertTrue(user.isPresent());
        assertEquals("admin", user.get().getUsername());
        assertEquals(Role.ADMIN, user.get().getRole());
    }

    @Test
    public void testAuthenticateInvalidPassword() {
        Optional<User> user = userDAO.authenticate("admin", "wrongpassword");
        assertFalse(user.isPresent());
    }

    @Test
    public void testGetUserById() {
        Optional<User> user = userDAO.getUserById(1);
        assertTrue(user.isPresent());
    }

    @Test
    public void testGetAllUsers() {
        var users = userDAO.getAllUsers();
        assertTrue(users.size() > 0);
    }
}
