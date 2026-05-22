package com.labflow.util;

import com.labflow.model.Role;
import com.labflow.model.User;
import com.labflow.model.Lab;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private Lab currentLab;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.currentLab = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        currentUser = null;
        currentLab = null;
    }

    public Lab getCurrentLab() {
        return currentLab;
    }

    public void setCurrentLab(Lab currentLab) {
        this.currentLab = currentLab;
    }

    public int getCurrentLabId() {
        return currentLab == null ? -1 : currentLab.getId();
    }

    public Role getEffectiveRole() {
        if (currentLab != null && currentLab.getMemberRole() != null) {
            return currentLab.getMemberRole();
        }
        return currentUser == null ? Role.GUEST : currentUser.getRole();
    }

    public boolean isLabOwner() {
        return currentUser != null
                && currentLab != null
                && currentLab.getCreatedByUserId() == currentUser.getId();
    }

    public boolean isAdmin() {
        return currentUser != null && getEffectiveRole() == Role.ADMIN;
    }

    public boolean isProfessor() {
        return currentUser != null && getEffectiveRole() == Role.PROFESSOR;
    }

    public boolean isTechnician() {
        return currentUser != null && getEffectiveRole() == Role.TECHNICIAN;
    }

    public boolean isGuest() {
        return currentUser != null && getEffectiveRole() == Role.GUEST;
    }

    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
}
