package seucxxy.csd.backend.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import seucxxy.csd.backend.common.entity.SubjectsEn;
import seucxxy.csd.backend.common.mapper.SubjectsEnMapper;

import java.util.List;

@Service
public class SubjectsEnService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubjectsEnService.class);
    
    @Autowired
    private SubjectsEnMapper subjectsEnMapper;
    
    public List<SubjectsEn> getAllSubjects() {
        System.out.println("---------- Service: 开始查询所有科目 ----------");
        List<SubjectsEn> result = subjectsEnMapper.findAll();
        System.out.println("Service: 查询结果: " + result);
        System.out.println("Service: 结果数量: " + (result != null ? result.size() : "null"));
        System.out.println("--------------------------------------------");
        return result;
    }
    
    public SubjectsEn getSubjectById(Long id) {
        return subjectsEnMapper.findById(id);
    }
    
    public SubjectsEn getSubjectByName(String name) {
        return subjectsEnMapper.findByName(name);
    }
    
    public void deleteAllSubjects() {
        // 通过Mapper删除所有数据
        subjectsEnMapper.deleteAll();
    }
    
    public void initTestData() {
        // 插入测试数据
        subjectsEnMapper.insert(new SubjectsEn("Mathematics"));
        subjectsEnMapper.insert(new SubjectsEn("Physics"));
        subjectsEnMapper.insert(new SubjectsEn("Chemistry"));
        subjectsEnMapper.insert(new SubjectsEn("Biology"));
        subjectsEnMapper.insert(new SubjectsEn("Computer Science"));
        subjectsEnMapper.insert(new SubjectsEn("English"));
        subjectsEnMapper.insert(new SubjectsEn("History"));
        subjectsEnMapper.insert(new SubjectsEn("Geography"));
    }
}