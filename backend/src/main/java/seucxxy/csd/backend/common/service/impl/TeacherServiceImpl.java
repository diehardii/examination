package seucxxy.csd.backend.common.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seucxxy.csd.backend.common.dto.TeacherProfileDto;
import seucxxy.csd.backend.common.entity.EducationStage;
import seucxxy.csd.backend.common.entity.Teacher;
import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.mapper.ClassMapper;
import seucxxy.csd.backend.common.mapper.EducationStageMapper;
import seucxxy.csd.backend.common.mapper.TeacherMapper;
import seucxxy.csd.backend.common.mapper.UserMapper;
import seucxxy.csd.backend.common.service.TeacherService;

@Service
@Transactional
public class TeacherServiceImpl implements TeacherService {

    private final UserMapper userMapper;
    private final TeacherMapper teacherMapper;
    private final EducationStageMapper educationStageMapper;
    private final ClassMapper classMapper;

    public TeacherServiceImpl(UserMapper userMapper,
                              TeacherMapper teacherMapper,
                              EducationStageMapper educationStageMapper,
                              ClassMapper classMapper) {
        this.userMapper = userMapper;
        this.teacherMapper = teacherMapper;
        this.educationStageMapper = educationStageMapper;
        this.classMapper = classMapper;
    }

    @Override
    public List<TeacherProfileDto> listTeachers() {
        List<User> teacherUsers = userMapper.findUsersByRoleName("TEACHER");
        
        return teacherUsers.stream().map(user -> {
            TeacherProfileDto dto = new TeacherProfileDto();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setRealName(user.getRealName());
            dto.setPhone(user.getPhone());

            // 获取教师的所有学段
            List<Teacher> stages = teacherMapper.findStagesByTeacherId(user.getId());
            List<Map<String, Object>> stageCounts = teacherMapper.countClassesByStage(user.getId());
            Map<Integer, Long> countMap = stageCounts.stream()
                    .collect(Collectors.toMap(
                            m -> (Integer) m.get("stage_id"),
                            m -> ((Number) m.get("count")).longValue()
                    ));

            List<TeacherProfileDto.StageInfo> stageInfos = stages.stream().map(stage -> {
                TeacherProfileDto.StageInfo info = new TeacherProfileDto.StageInfo();
                info.setStageId(stage.getStageId());
                info.setStageName(stage.getStageName());
                info.setClassCount(countMap.getOrDefault(stage.getStageId(), 0L).intValue());
                return info;
            }).collect(Collectors.toList());

            dto.setStages(stageInfos);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Integer> getTeacherClassesByStage(Long teacherId, Integer stageId) {
        validateTeacherUser(teacherId);
        validateStage(stageId);
        List<Integer> ids = teacherMapper.findClassIdsByStage(teacherId, stageId);
        return ids == null ? Collections.emptyList() : ids;
    }

    @Override
    public void assignClassesByStage(Long teacherId, Integer stageId, List<Integer> classIds) {
        validateTeacherUser(teacherId);
        validateStage(stageId);

        List<Integer> ids = classIds == null ? Collections.emptyList() : classIds;
        
        // 验证所有班级都属于指定学段
        if (!ids.isEmpty()) {
            int count = classMapper.countByStage(stageId, ids);
            if (count != ids.size()) {
                throw new IllegalArgumentException("Some classes do not belong to the specified stage.");
            }
        }

        // 删除该学段下的旧分配
        teacherMapper.deleteAssignmentsByStage(teacherId, stageId);

        // 插入新的分配
        if (!ids.isEmpty()) {
            List<Teacher> assignments = ids.stream().map(classId -> {
                Teacher assignment = new Teacher();
                assignment.setTeacherId(teacherId);
                assignment.setStageId(stageId);
                assignment.setClassId(classId);
                return assignment;
            }).collect(Collectors.toList());
            teacherMapper.insertAssignments(assignments);
        }
    }

    @Override
    public Map<Integer, List<Integer>> getTeacherAllAssignments(Long teacherId) {
        validateTeacherUser(teacherId);
        
        // 获取所有学段
        List<Teacher> stages = teacherMapper.findStagesByTeacherId(teacherId);
        
        // 为每个学段获取班级列表
        return stages.stream().collect(Collectors.toMap(
                Teacher::getStageId,
                stage -> teacherMapper.findClassIdsByStage(teacherId, stage.getStageId())
        ));
    }

    private void validateTeacherUser(Long teacherId) {
        User user = userMapper.findUserById(teacherId);
        if (user == null) {
            throw new IllegalArgumentException("Teacher user not found: " + teacherId);
        }
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
        if (!"TEACHER".equalsIgnoreCase(roleName)) {
            throw new IllegalArgumentException("User is not a teacher: " + teacherId);
        }
    }

    private void validateStage(Integer stageId) {
        EducationStage stage = educationStageMapper.findById(stageId);
        if (stage == null) {
            throw new IllegalArgumentException("Stage not found: " + stageId);
        }
    }
}
