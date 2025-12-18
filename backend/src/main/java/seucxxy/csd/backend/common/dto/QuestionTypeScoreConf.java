package seucxxy.csd.backend.common.dto;

public class QuestionTypeScoreConf {
    private Integer countOfQuestionType; // 一共有几道该题型的题目
    private Integer numberOfQuestionTypeQuestions; // 该题型的题目的小题一共有多少题
    private Integer scoreOfQuestionType; // 该题型总分

    public Integer getCountOfQuestionType() {
        return countOfQuestionType;
    }

    public void setCountOfQuestionType(Integer countOfQuestionType) {
        this.countOfQuestionType = countOfQuestionType;
    }

    public Integer getNumberOfQuestionTypeQuestions() {
        return numberOfQuestionTypeQuestions;
    }

    public void setNumberOfQuestionTypeQuestions(Integer numberOfQuestionTypeQuestions) {
        this.numberOfQuestionTypeQuestions = numberOfQuestionTypeQuestions;
    }

    public Integer getScoreOfQuestionType() {
        return scoreOfQuestionType;
    }

    public void setScoreOfQuestionType(Integer scoreOfQuestionType) {
        this.scoreOfQuestionType = scoreOfQuestionType;
    }
}
