package seucxxy.csd.backend.common.mapper;

import org.apache.ibatis.annotations.Mapper;

import seucxxy.csd.backend.common.entity.SubjectsEn;

import java.util.List;

@Mapper
public interface SubjectsEnMapper {
    List<SubjectsEn> findAll();
    SubjectsEn findById(Long id);
    SubjectsEn findByName(String name);
    void deleteAll();
    void insert(SubjectsEn subject);
}