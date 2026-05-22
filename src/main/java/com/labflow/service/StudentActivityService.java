package com.labflow.service;

import com.labflow.dao.BorrowRecordDAO;
import com.labflow.dao.FaultReportDAO;
import com.labflow.dao.GamificationDAO;
import com.labflow.model.BorrowRecord;
import com.labflow.model.Role;
import com.labflow.model.StudentActivitySummary;
import com.labflow.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentActivityService {
    private final BorrowRecordDAO borrowRecordDAO = new BorrowRecordDAO();
    private final FaultReportDAO faultReportDAO = new FaultReportDAO();
    private final GamificationDAO gamificationDAO = new GamificationDAO();
    private final LabService labService = new LabService();

    public StudentActivitySummary getSummaryForUser(int userId, int labId) {
        StudentActivitySummary summary = new StudentActivitySummary();
        summary.setUserId(userId);
        User user = labService.getMembers(labId).stream()
                .filter(member -> member.getId() == userId)
                .findFirst()
                .orElse(null);
        summary.setUsername(user == null ? "User " + userId : user.getUsername());
        List<BorrowRecord> borrows = borrowRecordDAO.getBorrowsByUser(userId, labId);
        summary.setBorrowCount(borrows.size());
        summary.setActiveBorrows((int) borrows.stream().filter(BorrowRecord::isActive).count());
        summary.setOverdueCount((int) borrows.stream().filter(BorrowRecord::isOverdue).count());
        summary.setFaultReportsCount(faultReportDAO.getFaultReportsByUser(userId, labId).size());
        summary.setPoints(gamificationDAO.getPoints(userId, labId));
        return summary;
    }

    public List<StudentActivitySummary> getAllStudentsSummary(int labId) {
        List<User> members = labService.getMembers(labId).stream()
                .filter(user -> user.getRole() == Role.STUDENT || user.getRole() == Role.GUEST)
                .toList();
        Map<Integer, Integer> borrowCounts = borrowRecordDAO.getBorrowCountPerUser(labId);
        Map<Integer, Integer> faultCounts = faultReportDAO.getFaultCountPerUser(labId);
        List<StudentActivitySummary> result = new ArrayList<>();
        for (User user : members) {
            StudentActivitySummary summary = getSummaryForUser(user.getId(), labId);
            summary.setBorrowCount(borrowCounts.getOrDefault(user.getId(), summary.getBorrowCount()));
            summary.setFaultReportsCount(faultCounts.getOrDefault(user.getId(), summary.getFaultReportsCount()));
            result.add(summary);
        }
        result.sort(java.util.Comparator.comparingInt(StudentActivitySummary::getPoints).reversed()
                .thenComparing(StudentActivitySummary::getUsername, String.CASE_INSENSITIVE_ORDER));
        return result;
    }
}
