package com.lms.dto;

import lombok.Data;

/** Payload to update lesson progress (e.g. from video player heartbeat) */
@Data
public class LessonProgressRequest {
    private Long lessonId;
    /** Current playback position in seconds */
    private Integer currentPosition;
    /** Total duration of the video/content in seconds */
    private Integer totalDuration;
    /** Mark as fully completed */
    private boolean completed;
}
