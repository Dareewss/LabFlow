package com.labflow.api;

import com.google.gson.JsonObject;
import com.labflow.model.BorrowRecord;
import com.labflow.service.BorrowService;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class BorrowApiHandler {
    private final BorrowService borrowService = new BorrowService();

    public Map<String, Object> borrow(int equipmentId, JsonObject body) {
        int userId = ApiResponseUtil.integer(body, "userId");
        String expected = ApiResponseUtil.string(body, "expectedReturnDate");
        String notes = ApiResponseUtil.string(body, "notes");
        LocalDate expectedReturnDate = expected == null || expected.isBlank() ? null : LocalDate.parse(expected);
        Optional<BorrowRecord> record = borrowService.borrowEquipment(equipmentId, userId, expectedReturnDate, notes);
        return record.map(this::toMap).orElseGet(LinkedHashMap::new);
    }

    public Map<String, Object> returnEquipment(JsonObject body) {
        int borrowRecordId = ApiResponseUtil.integer(body, "borrowRecordId");
        String condition = ApiResponseUtil.string(body, "returnCondition");
        String notes = ApiResponseUtil.string(body, "notes");
        String defectDescription = ApiResponseUtil.string(body, "defectDescription");
        borrowService.returnEquipment(borrowRecordId, condition == null ? "GOOD" : condition, notes, defectDescription);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("borrowRecordId", borrowRecordId);
        result.put("status", "returned");
        return result;
    }

    public Optional<BorrowRecord> activeBorrowForEquipment(int equipmentId) {
        return borrowService.getActiveBorrow(equipmentId);
    }

    private Map<String, Object> toMap(BorrowRecord record) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", record.getId());
        data.put("equipmentId", record.getEquipmentId());
        data.put("userId", record.getUserId());
        data.put("equipmentName", record.getEquipmentName());
        data.put("username", record.getUsername());
        data.put("borrowDate", value(record.getBorrowDate()));
        data.put("expectedReturnDate", value(record.getExpectedReturnDate()));
        data.put("actualReturnDate", value(record.getActualReturnDate()));
        data.put("status", record.getStatus());
        data.put("returnCondition", record.getReturnCondition());
        data.put("notes", record.getNotes());
        return data;
    }

    private String value(Object value) {
        return value == null ? null : value.toString();
    }
}
