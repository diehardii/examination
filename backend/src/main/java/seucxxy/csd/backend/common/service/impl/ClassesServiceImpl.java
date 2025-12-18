package seucxxy.csd.backend.common.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seucxxy.csd.backend.common.entity.ClassInfo;
import seucxxy.csd.backend.common.mapper.ClassMapper;
import seucxxy.csd.backend.common.service.ClassesService;

import java.util.List;

@Service
@Transactional
public class ClassesServiceImpl implements ClassesService {

    private final ClassMapper classMapper;

    public ClassesServiceImpl(ClassMapper classMapper) {
        this.classMapper = classMapper;
    }

    @Override
    public List<ClassInfo> listAll() {
        return classMapper.findAll();
    }

    @Override
    public List<ClassInfo> listByStage(Integer stageId) {
        return classMapper.findByStage(stageId);
    }

    @Override
    public ClassInfo create(ClassInfo classInfo) {
        classMapper.insert(classInfo);
        return classInfo;
    }

    @Override
    public void update(ClassInfo classInfo) {
        classMapper.update(classInfo);
    }

    @Override
    public void delete(Integer classId) {
        classMapper.delete(classId);
    }
}
