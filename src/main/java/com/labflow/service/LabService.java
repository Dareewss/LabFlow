package com.labflow.service;

import com.labflow.dao.LabDAO;
import com.labflow.dao.UserDAO;
import com.labflow.model.Lab;
import com.labflow.model.Role;
import com.labflow.model.User;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class LabService {
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final LabDAO labDAO = new LabDAO();
    private final UserDAO userDAO = new UserDAO();

    public List<Lab> getLabsForUser(int userId) {
        return labDAO.findByUserId(userId);
    }

    public Optional<Lab> createLab(int userId, String name) {
        validateName(name);
        int id = labDAO.insert(name.trim(), userId, false, generateCode());
        labDAO.addMember(id, userId, Role.ADMIN);
        return labDAO.findByUserId(userId).stream().filter(lab -> lab.getId() == id).findFirst();
    }

    public Optional<Lab> createProtectedTestLabForUser(int userId) {
        int id = labDAO.insert("Test Lab", userId, true, generateCode());
        labDAO.addMember(id, userId, Role.ADMIN);
        return labDAO.findByUserId(userId).stream().filter(lab -> lab.getId() == id).findFirst();
    }

    public Optional<Lab> joinByInviteCode(int userId, String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            return Optional.empty();
        }
        Lab lab = labDAO.findByInviteCode(inviteCode.trim().toUpperCase(Locale.ROOT));
        if (lab == null) {
            throw new IllegalArgumentException("Invite code invalid.");
        }
        labDAO.addMember(lab.getId(), userId, Role.GUEST);
        return Optional.ofNullable(labDAO.findByIdForUser(lab.getId(), userId));
    }

    public Optional<User> createUser(String username, String password, String fullName) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required.");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(password);
        user.setFullName(fullName);
        user.setRole(Role.STUDENT);
        int id = userDAO.createUserAndReturnId(user);
        if (id <= 0) {
            throw new IllegalArgumentException("Could not create user. Username may already exist.");
        }
        createProtectedTestLabForUser(id);
        return userDAO.getUserById(id);
    }

    public Optional<User> createGuestUser(int labId, String username, String password, String fullName) {
        requireOwner(labId);
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required.");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(password);
        user.setFullName(fullName);
        user.setRole(Role.GUEST);
        int id = userDAO.createUserAndReturnId(user);
        if (id <= 0) {
            throw new IllegalArgumentException("Could not create guest. Username may already exist.");
        }
        labDAO.addMember(labId, id, Role.GUEST);
        return userDAO.getUserById(id);
    }

    public List<User> getMembers(int labId) {
        return labDAO.findMembers(labId);
    }

    public List<User> getUsersOutsideLab(int labId) {
        return userDAO.getAllUsers().stream()
                .filter(user -> !labDAO.isMember(labId, user.getId()))
                .toList();
    }

    public void addExistingMember(int labId, int userId, Role role) {
        requireOwner(labId);
        Role safeRole = role == null ? Role.STUDENT : role;
        labDAO.addMember(labId, userId, safeRole);
    }

    public void addExistingMemberByUsername(int labId, String username, Role role) {
        requireOwner(labId);
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        User user = userDAO.getUserByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("No account found with that username."));
        if (labDAO.isMember(labId, user.getId())) {
            throw new IllegalArgumentException("That account is already a member of this lab.");
        }
        labDAO.addMember(labId, user.getId(), role == null ? Role.STUDENT : role);
    }

    public void updateMemberRole(int labId, int userId, Role role) {
        requireOwner(labId);
        if (role == null) {
            throw new IllegalArgumentException("Choose a role first.");
        }
        Lab lab = labDAO.findById(labId);
        if (lab != null && lab.getCreatedByUserId() == userId) {
            throw new IllegalArgumentException("The lab owner keeps the owner/admin access.");
        }
        labDAO.updateMemberRole(labId, userId, role);
    }

    public void removeMember(int labId, int userId) {
        requireOwner(labId);
        Lab lab = labDAO.findById(labId);
        if (lab != null && lab.getCreatedByUserId() == userId) {
            throw new IllegalArgumentException("The lab owner cannot be removed from their lab.");
        }
        labDAO.removeMember(labId, userId);
    }

    public void renameLab(int labId, String name) {
        requireOwner(labId);
        validateName(name);
        labDAO.rename(labId, name.trim());
    }

    public void updateColorPalette(int labId, ThemeManager.ColorPalette palette) {
        if (labId <= 0) {
            throw new IllegalArgumentException("Select a lab before changing its theme.");
        }
        labDAO.updateColorPalette(labId, (palette == null ? ThemeManager.ColorPalette.RED : palette).name());
    }

    public void deleteLab(Lab lab) {
        if (lab == null) {
            return;
        }
        requireOwner(lab.getId());
        if (lab.isProtectedLab()) {
            throw new IllegalArgumentException("Test Lab can be renamed, but cannot be deleted.");
        }
        labDAO.delete(lab.getId());
    }

    public boolean isOwner(Lab lab) {
        return lab != null && SessionManager.getInstance().getCurrentUserId() == lab.getCreatedByUserId();
    }

    public boolean isCurrentUserOwner(int labId) {
        Lab lab = labDAO.findById(labId);
        return lab != null && SessionManager.getInstance().getCurrentUserId() == lab.getCreatedByUserId();
    }

    private void requireOwner(int labId) {
        if (!isCurrentUserOwner(labId)) {
            throw new IllegalArgumentException("Only the lab owner can manage this lab.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Lab name is required.");
        }
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder("LAB-");
        for (int i = 0; i < 8; i++) {
            builder.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return builder.toString();
    }
}
