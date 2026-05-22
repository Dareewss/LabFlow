package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.EquipmentContainerDAO;
import com.labflow.model.EquipmentContainer;
import com.labflow.util.SessionManager;

import java.util.List;
import java.util.Locale;

public class EquipmentContainerService {
    private final EquipmentContainerDAO containerDAO = new EquipmentContainerDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public List<EquipmentContainer> getContainers() {
        return containerDAO.findAll();
    }

    public EquipmentContainer createContainer(String requestedName) {
        String name = uniqueName(normalizedName(requestedName), 0);
        int id = containerDAO.insert(name);
        log("CREATE_CONTAINER", id, "Created container " + name);
        return containerDAO.findById(id);
    }

    public EquipmentContainer findContainerByName(String requestedName) {
        String name = normalizedName(requestedName);
        return getContainers().stream()
                .filter(container -> container.getName() != null
                        && container.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public EquipmentContainer getOrCreateContainer(String requestedName) {
        EquipmentContainer existing = findContainerByName(requestedName);
        return existing == null ? createContainer(requestedName) : existing;
    }

    public void renameContainer(int containerId, String requestedName) {
        String name = uniqueName(normalizedName(requestedName), containerId);
        containerDAO.rename(containerId, name);
        log("RENAME_CONTAINER", containerId, "Renamed container to " + name);
    }

    public int deleteContainer(int containerId) {
        EquipmentContainer container = containerDAO.findById(containerId);
        if (container == null) {
            throw new IllegalArgumentException("Container not found");
        }
        int movedItems = containerDAO.delete(containerId);
        log("DELETE_CONTAINER", containerId, "Deleted container " + container.getName() + " and moved " + movedItems + " items to No Container");
        return movedItems;
    }

    private String uniqueName(String baseName, int currentContainerId) {
        List<EquipmentContainer> containers = containerDAO.findAll();
        String candidate = baseName;
        int index = 2;
        while (containsName(containers, candidate, currentContainerId)) {
            candidate = baseName + " " + index;
            index++;
        }
        return candidate;
    }

    private boolean containsName(List<EquipmentContainer> containers, String candidate, int currentContainerId) {
        String normalized = candidate.toLowerCase(Locale.ROOT);
        return containers.stream().anyMatch(container ->
                container.getId() != currentContainerId
                        && container.getName() != null
                        && container.getName().toLowerCase(Locale.ROOT).equals(normalized));
    }

    private String normalizedName(String value) {
        String name = value == null ? "" : value.trim();
        return name.isBlank() ? "Container" : name;
    }

    private void log(String action, int entityId, String description) {
        int userId = SessionManager.getInstance().getCurrentUserId();
        activityLogDAO.log(userId > 0 ? userId : null, action, "CONTAINER", entityId, description);
    }
}
