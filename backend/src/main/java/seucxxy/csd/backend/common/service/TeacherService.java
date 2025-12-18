package seucxxy.csd.backend.common.service;

import java.util.List;
import java.util.Map;
import seucxxy.csd.backend.common.dto.TeacherProfileDto;

public interface TeacherService {
    /**
     * 获取所有教师的列表（包含其所有学段信息）
     */
    List<TeacherProfileDto> listTeachers();
    
    /**
     * 获取教师在某个学段下的班级ID列表
     */
    List<Integer> getTeacherClassesByStage(Long teacherId, Integer stageId);
    
    /**
     * 为教师分配某个学段下的班级
     * @param teacherId 教师ID
     * @param stageId 学段ID
     * @param classIds 班级ID列表
     */
    void assignClassesByStage(Long teacherId, Integer stageId, List<Integer> classIds);
    
    /**
     * 获取教师的所有学段和班级分配情况
     */
    Map<Integer, List<Integer>> getTeacherAllAssignments(Long teacherId);
}
