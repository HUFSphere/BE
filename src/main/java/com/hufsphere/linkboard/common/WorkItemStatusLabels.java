package com.hufsphere.linkboard.common;

public final class WorkItemStatusLabels {

    private WorkItemStatusLabels() {
    }

    // status가 4단계(todo/in_progress/review/done) 밖의 값이면 원본 status를 그대로 반환한다
    public static String resolve(String status, String lang) {
        if (status == null) {
            return null;
        }

        boolean ko = "ko".equals(lang);
        return switch (status) {
            case "todo" -> ko ? "할일" : "To Do";
            case "in_progress" -> ko ? "진행중" : "In Progress";
            case "review" -> ko ? "리뷰중" : "Review";
            case "done" -> ko ? "완료" : "Done";
            default -> status;
        };
    }
}
