package com.labflow.service;

import com.labflow.model.Equipment;
import com.labflow.model.EquipmentContainer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TestKitImportService {
    private final EquipmentContainerService containerService = new EquipmentContainerService();
    private final EquipmentService equipmentService = new EquipmentService();

    public ImportSummary importDefaultKits() {
        List<KitTemplate> templates = List.of(
                new KitTemplate("Robotics Starter Kit", "Robotics Storage",
                        new ItemTemplate("Arduino Uno R4", "Robotics", "Core microcontroller board", "Open hardware controller", "ARD-R4", "robotics, starter", 180),
                        new ItemTemplate("Ultrasonic Sensor HC-SR04", "Robotics", "Distance sensor", "Obstacle detection and ranging", "HCSR04", "sensor, robotics", 180),
                        new ItemTemplate("Servo Motor SG90", "Robotics", "Servo actuator", "Compact servo for robotics projects", "SG90", "actuator, robotics", 120),
                        new ItemTemplate("Breadboard 830 Tie Points", "Electronics", "Prototyping board", "Reusable prototyping board", "BB-830", "prototype, electronics", 365)
                ),
                new KitTemplate("Electronics Measurement Kit", "Electronics Cabinet",
                        new ItemTemplate("Digital Multimeter", "Electronics", "Measurement tool", "Measures voltage, current, and resistance", "DMM-6000", "measurement, diagnostics", 180),
                        new ItemTemplate("USB Power Meter", "Electronics", "Power meter", "USB inline power usage meter", "USB-PM", "measurement, power", 180),
                        new ItemTemplate("Oscilloscope Probe", "Electronics", "Probe accessory", "Passive probe for bench scope", "OSP-10X", "probe, electronics", 180),
                        new ItemTemplate("Soldering Stand", "Electronics", "Workbench tool", "Stand with sponge tray", "SOL-STD", "soldering, bench", 365)
                ),
                new KitTemplate("Chemistry Safety Kit", "Chemistry Prep Room",
                        new ItemTemplate("Safety Goggles Set", "Chemistry", "Protective equipment", "Shared classroom safety goggles set", "SAFE-GOG", "safety, chemistry", 120),
                        new ItemTemplate("Beaker Set 250ml", "Chemistry", "Glassware", "Reusable borosilicate beaker set", "BK-250", "glassware, chemistry", 180),
                        new ItemTemplate("Test Tube Rack", "Chemistry", "Lab support", "Rack for classroom experiments", "TTR-01", "chemistry, support", 365),
                        consumable("pH Indicator Strips", "Chemistry", "Chemistry Safety Kit", 50, 10, "strips", "consumable, chemistry")
                )
        );

        int createdContainers = 0;
        int createdEquipment = 0;
        int repairedContainers = 0;
        int skippedEquipment = 0;
        long batchSeed = System.currentTimeMillis();
        for (int kitIndex = 0; kitIndex < templates.size(); kitIndex++) {
            KitTemplate template = templates.get(kitIndex);
            List<EquipmentContainer> containers = findExistingKitContainers(template.containerName());
            if (containers.isEmpty()) {
                containers = List.of(containerService.createContainer(template.containerName()));
                createdContainers++;
            } else {
                repairedContainers += containers.size();
            }
            for (EquipmentContainer container : containers) {
                for (int itemIndex = 0; itemIndex < template.items().size(); itemIndex++) {
                    ItemTemplate item = template.items().get(itemIndex);
                    Equipment existingItem = findExistingKitItem(container.getId(), item);
                    if (existingItem != null) {
                        equipmentService.replaceTags(existingItem.getId(), item.tags());
                        skippedEquipment++;
                        continue;
                    }
                    Equipment equipment = new Equipment();
                    equipment.setName(item.name());
                    equipment.setCategory(item.category());
                    equipment.setLocation(template.location());
                    equipment.setDescription(item.description());
                    equipment.setNotes(item.notes());
                    equipment.setManufacturer("LabFlow Demo");
                    equipment.setModel(item.model());
                    equipment.setContainerId(container.getId());
                    equipment.setPurchaseDate(LocalDate.now().minusDays(30L + itemIndex));
                    equipment.setLastMaintenanceDate(LocalDate.now().minusDays(15L + itemIndex));
                    equipment.setMaintenanceIntervalDays(item.maintenanceIntervalDays());
                    equipment.setNextMaintenanceDate(item.maintenanceIntervalDays() == null ? null : LocalDate.now().plusDays(item.maintenanceIntervalDays()));
                    equipment.setItemType(item.itemType());
                    equipment.setQuantity(item.quantity());
                    equipment.setMinimumQuantity(item.minimumQuantity());
                    equipment.setUnit(item.unit());
                    equipment.setSerialNumber("KIT-" + kitIndex + "-" + itemIndex + "-" + batchSeed + "-" + container.getId());
                    Equipment created = equipmentService.addEquipment(equipment)
                            .orElseThrow(() -> new IllegalStateException("Could not create kit item: " + item.name()));
                    equipmentService.replaceTags(created.getId(), item.tags());
                    createdEquipment++;
                }
            }
        }
        return new ImportSummary(createdContainers, repairedContainers, createdEquipment, skippedEquipment);
    }

    private List<EquipmentContainer> findExistingKitContainers(String baseName) {
        return containerService.getContainers().stream()
                .filter(container -> isKitContainerName(container.getName(), baseName))
                .toList();
    }

    private boolean isKitContainerName(String name, String baseName) {
        if (name == null || baseName == null) {
            return false;
        }
        if (name.equalsIgnoreCase(baseName)) {
            return true;
        }
        return name.matches("(?i)" + java.util.regex.Pattern.quote(baseName) + "\\s+\\d+");
    }

    private Equipment findExistingKitItem(int containerId, ItemTemplate item) {
        return equipmentService.getAllEquipment().stream()
                .filter(equipment -> equipment.getContainerId() != null && equipment.getContainerId() == containerId)
                .filter(equipment -> same(equipment.getName(), item.name()))
                .filter(equipment -> item.model() == null || same(equipment.getModel(), item.model()))
                .findFirst()
                .orElse(null);
    }

    private boolean same(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private static ItemTemplate consumable(String name, String category, String model, int quantity, int minimumQuantity, String unit, String tags) {
        return new ItemTemplate(name, category, "Consumable stock", "Imported demo consumable item", model, tags, null, "CONSUMABLE", quantity, minimumQuantity, unit);
    }

    public record ImportSummary(int containerCount, int repairedContainerCount, int equipmentCount, int skippedEquipmentCount) {
    }

    private record KitTemplate(String containerName, String location, List<ItemTemplate> items) {
        private KitTemplate(String containerName, String location, ItemTemplate... items) {
            this(containerName, location, new ArrayList<>(List.of(items)));
        }
    }

    private record ItemTemplate(
            String name,
            String category,
            String description,
            String notes,
            String model,
            String tags,
            Integer maintenanceIntervalDays,
            String itemType,
            int quantity,
            int minimumQuantity,
            String unit
    ) {
        private ItemTemplate(String name, String category, String description, String notes, String model, String tags, Integer maintenanceIntervalDays) {
            this(name, category, description, notes, model, tags, maintenanceIntervalDays, "ASSET", 1, 0, null);
        }
    }
}
