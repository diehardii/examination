package seucxxy.csd.backend.common.service.impl;

import org.springframework.stereotype.Service;
import seucxxy.csd.backend.common.entity.EducationStage;
import seucxxy.csd.backend.common.mapper.EducationStageMapper;
import seucxxy.csd.backend.common.service.EducationStageService;

import java.util.List;

@Service
public class EducationStageServiceImpl implements EducationStageService {

    private final EducationStageMapper educationStageMapper;

    public EducationStageServiceImpl(EducationStageMapper educationStageMapper) {
        this.educationStageMapper = educationStageMapper;
    }

    @Override
    public List<EducationStage> listAll() {
        return educationStageMapper.findAll();
    }
}
