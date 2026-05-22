package com.labflow.service;

import com.labflow.dao.GamificationDAO;

public class GamificationService {
    public static final int POINTS_RETURN_ON_TIME = 10;
    public static final int POINTS_REPORT_FAULT = 5;
    public static final int POINTS_RETURN_LATE_PENALTY = -5;
    public static final int POINTS_RETURN_DEFECT_PENALTY = -10;

    private final GamificationDAO gamificationDAO = new GamificationDAO();

    public void rewardReturnOnTime(int userId, int labId) {
        gamificationDAO.addPoints(userId, labId, POINTS_RETURN_ON_TIME, "Returnare la timp");
    }

    public void rewardFaultReport(int userId, int labId) {
        gamificationDAO.addPoints(userId, labId, POINTS_REPORT_FAULT, "Raportare defecțiune");
    }

    public void penaltyLateReturn(int userId, int labId) {
        gamificationDAO.addPoints(userId, labId, POINTS_RETURN_LATE_PENALTY, "Penalizare returnare întârziată");
    }

    public void penaltyDefectReturn(int userId, int labId) {
        gamificationDAO.addPoints(userId, labId, POINTS_RETURN_DEFECT_PENALTY, "Penalizare echipament returnat defect");
    }
}
