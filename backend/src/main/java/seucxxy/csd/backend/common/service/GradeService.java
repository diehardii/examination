package seucxxy.csd.backend.common.service;

import seucxxy.csd.backend.common.entity.Grade;

import java.util.List;

public interface GradeService {
    List<Grade> listAll();
    List<Grade> listByStage(Integer stageId);
}
