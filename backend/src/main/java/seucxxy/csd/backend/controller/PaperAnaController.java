package seucxxy.csd.backend.controller;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.entity.User;
import seucxxy.csd.backend.entity.UserTestRecord;
import seucxxy.csd.backend.service.PaperAnaService;
import seucxxy.csd.backend.service.SubjectService;
import seucxxy.csd.backend.service.UserTestRecordService;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/exam-paper")
public class PaperAnaController {

    private final PaperAnaService paperAnaService;
    private final SubjectService subjectService;
    private final UserTestRecordService userTestRecordService;

    public PaperAnaController(PaperAnaService paperAnaService,
                              SubjectService subjectService,
                              UserTestRecordService userTestRecordService) {
        this.paperAnaService = paperAnaService;
        this.subjectService = subjectService;
        this.userTestRecordService = userTestRecordService;
    }

    @GetMapping("/user-subjects")
    public List<String> getUserSubjects(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return subjectService.getSubjectsForUser(user.getUsername());
    }

    @GetMapping("/records")
    public List<UserTestRecord> getExamRecords(@RequestParam String subject,
                                              HttpSession session) {
        User user = (User) session.getAttribute("user");
        return userTestRecordService.getUserTestRecordsByUsernameAndSubject(
                user.getUsername(), subject);

    }

    @PostMapping("/analyze")
    public String analyzeSubject(@RequestParam String subject,
                                 HttpSession session) {
        User user = (User) session.getAttribute("user");
        return paperAnaService.analyzeSubject(user.getUsername(), subject);
    }
}