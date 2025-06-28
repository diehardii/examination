package seucxxy.csd.backend.dto;


import lombok.Data;

@Data
public class UserExamPaperDTO {

        private Long userId;
        private String userName;
        private String realName;
        private String university;
        private String department;
        private int testCount;

}
