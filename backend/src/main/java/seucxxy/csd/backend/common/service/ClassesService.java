package seucxxy.csd.backend.common.service;

import seucxxy.csd.backend.common.entity.ClassInfo;

import java.util.List;

public interface ClassesService {
    List<ClassInfo> listAll();
    List<ClassInfo> listByStage(Integer stageId);
    ClassInfo create(ClassInfo classInfo);
    void update(ClassInfo classInfo);
    void delete(Integer classId);
}
