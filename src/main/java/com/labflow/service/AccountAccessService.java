package com.labflow.service;

import com.labflow.dao.UserDAO;
import com.labflow.model.User;

import java.util.Optional;

public class AccountAccessService {
    private final UserDAO userDAO = new UserDAO();
    private final LabService labService = new LabService();

    public Optional<User> authenticate(String username, String password) {
        return userDAO.authenticate(username, password);
    }

    public Optional<User> authenticateByUserId(int userId, String password) {
        return userDAO.authenticateByUserId(userId, password);
    }

    public Optional<User> getUserById(int userId) {
        return userDAO.getUserById(userId);
    }

    public User registerUser(String username, String password, String fullName) {
        return labService.createUser(username, password, fullName)
                .orElseThrow(() -> new IllegalArgumentException("Could not create user."));
    }
}
