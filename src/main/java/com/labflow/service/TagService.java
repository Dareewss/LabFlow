package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.TagDAO;
import com.labflow.model.Tag;
import com.labflow.util.SessionManager;

import java.util.Arrays;
import java.util.List;

public class TagService {
    private final TagDAO tagDAO = new TagDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public List<Tag> getTags() {
        return tagDAO.findAll();
    }

    public List<String> getTagNames() {
        return tagDAO.findAll().stream().map(Tag::getName).toList();
    }

    public List<String> getEquipmentTagNames(int equipmentId) {
        return tagDAO.findNamesForEquipment(equipmentId);
    }

    public void replaceEquipmentTags(int equipmentId, String commaSeparatedTags) {
        if (!SessionManager.getInstance().isAdmin() && !SessionManager.getInstance().isTechnician()) {
            throw new IllegalArgumentException("Only admins and technicians can change tags.");
        }
        List<String> names = parse(commaSeparatedTags);
        tagDAO.replaceEquipmentTags(equipmentId, names);
        int userId = SessionManager.getInstance().getCurrentUserId();
        activityLogDAO.log(userId > 0 ? userId : null, "UPDATE_EQUIPMENT_TAGS", "EQUIPMENT", equipmentId,
                "Updated equipment tags: " + String.join(", ", names));
    }

    public int createTag(String name, String color) {
        if (!SessionManager.getInstance().isAdmin() && !SessionManager.getInstance().isTechnician()) {
            throw new IllegalArgumentException("Only admins and technicians can create tags.");
        }
        int id = tagDAO.createIfMissing(name, color);
        int userId = SessionManager.getInstance().getCurrentUserId();
        activityLogDAO.log(userId > 0 ? userId : null, "CREATE_TAG", "TAG", id, "Created tag " + name);
        return id;
    }

    private List<String> parse(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .distinct()
                .toList();
    }
}
