package seucxxy.csd.backend.common.service.impl;

import org.springframework.stereotype.Service;
import seucxxy.csd.backend.common.entity.Grade;
import seucxxy.csd.backend.common.mapper.GradeMapper;
import seucxxy.csd.backend.common.service.GradeService;

import java.util.List;

@Service
public class GradeServiceImpl implements GradeService {

    private final GradeMapper gradeMapper;

    public GradeServiceImpl(GradeMapper gradeMapper) {
        this.gradeMapper = gradeMapper;
    }

    @Override
    public List<Grade> listAll() {
        return gradeMapper.findAll();
    }

    @Override
    public List<Grade> listByStage(Integer stageId) {
        return gradeMapper.findByStageId(stageId);
    }
}
