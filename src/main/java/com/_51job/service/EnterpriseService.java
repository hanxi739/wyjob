package com._51job.service;

import com._51job.dao.EnterpriseDao;
import com._51job.domain.*;
import com._51job.tool.DataUtil;
import com._51job.web.PostInfo;
import com._51job.web.PostInfoState;
import com._51job.web.ResumeInfo;
import com._51job.web.SimpleResume;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class EnterpriseService {

    private final EnterpriseDao enterpriseDao;

    @Autowired
    public EnterpriseService(EnterpriseDao enterpriseDao) {
        this.enterpriseDao = enterpriseDao;
    }

    //获取所发布的岗位列表以及状态
    public PostInfo getPost(int companyId){
        PostInfo post=new PostInfo();
        Enterprise en=enterpriseDao.get(Enterprise.class,companyId);
        //获得实际的地址、规模、行业
        String address=getActualAttribute(en.getDomicile(),1);
        String scale=getActualAttribute(en.getScale(),7);
        String industry=getActualAttribute(en.getIndustry(),2);
        en.setActualDomicile(address);
        en.setActualScale(scale);
        en.setActualIndustry(industry);
        post.setEnterprise(en);
        //获得企业发布的招聘信息
        List<Recruitment> list=enterpriseDao.getRecruitmentList(companyId);
        for (int i = 0; i < list.size(); i++) {
            //获得实际的岗位开放状态和时间
            String st=getRecruitState(list.get(i).getState());
            list.get(i).setActualState(st);
            String degree=getActualAttribute(list.get(i).getMinDegree(),9);
            list.get(i).setActualMinDegree(degree);
            String time=getRecruitTime(list.get(i).getTime());
            list.get(i).setActualTime(time);
        }
        post.setRecruitment(list);
        return post;
    }

    //岗位详情
    public PostInfoState getRecruitment(int id){
        PostInfoState postInfoState=new PostInfoState();
        postInfoState.setRecruitment(DataUtil.getRecruitment(id));
        postInfoState.setEnterprise(DataUtil.getEnterprise(postInfoState.getRecruitment().getEnterpriseId()));
        postInfoState.getRecruitment().setActualFunction(DataUtil.getDictionary(postInfoState.getRecruitment().getFunction()).getDictionaryName());
        postInfoState.getRecruitment().setActualWorkType(DataUtil.getDictionary(postInfoState.getRecruitment().getWorkType()).getDictionaryName());
        postInfoState.getRecruitment().setActualMinDegree(DataUtil.getDictionary(postInfoState.getRecruitment().getMinDegree()).getDictionaryName());
        postInfoState.getEnterprise().setActualDomicile(DataUtil.getDictionary(postInfoState.getEnterprise().getDomicile()).getDictionaryName());
        postInfoState.getEnterprise().setActualScale(DataUtil.getDictionary(postInfoState.getEnterprise().getScale()).getDictionaryName());
        postInfoState.getEnterprise().setActualIndustry(DataUtil.getDictionary(postInfoState.getEnterprise().getIndustry()).getDictionaryName());
        postInfoState.getEnterprise().setActualType(DataUtil.getDictionary(postInfoState.getEnterprise().getType()).getDictionaryName());
        return postInfoState;
    }

    //获取指定招聘信息投递来的简历列表
    public List<SimpleResume> getSpecificPost(int recruitmentId){
        List<SimpleResume> simpleList=new ArrayList<>();
        List<Application> applicationList=enterpriseDao.getSpecificApplicationList(recruitmentId);
        for (int i = 0; i < applicationList.size(); i++) {
            SimpleResume simpleResume=new SimpleResume();
            int applicantId=applicationList.get(i).getApplicantId();
            Applicant applicant=enterpriseDao.get(Applicant.class,applicantId);
            simpleResume.setApplicantId(applicantId);
            simpleResume.setName(applicant.getName());
            simpleResume.setGender(getPersonGender(applicant.getGender()));
            simpleResume.setWorkStatus(getCurrentWorkStatus(applicant.getWorkingStatus()));
            simpleResume.setApplicationTime(getActualTime(applicationList.get(i).getApplicationTime()));
            simpleResume.setApplicationState(getActualWorkState(applicationList.get(i).getApplicationState()));
            String degree=getHighestDegree(applicantId);
            simpleResume.setDegree(degree);
            simpleList.add(simpleResume);
        }
        return simpleList;
    }


    //获取指定的简历的详情
    public ResumeInfo getSpecificResume(int applicationId, boolean f){
        ResumeInfo resumeInfo=new ResumeInfo();
        int applicantId;
        if(f)applicantId=enterpriseDao.getApplicantId(applicationId);
        else applicantId=applicationId;
        Applicant applicant=getActualInfoApplicant(applicantId);
        List<Experience> expList=getSpecificExperience(applicantId);
        if(expList.size()>0) {
            List<EducationExperience> eduList = getActualStudyExperience(expList);
            List<WorkingExperience> workList = getActualWorkExperience(expList);
            resumeInfo.setExperienceList(expList);
            resumeInfo.setEducationList(eduList);
            resumeInfo.setWorkingList(workList);
        }
        else{
            resumeInfo.setExperienceList(null);
            resumeInfo.setEducationList(null);
            resumeInfo.setWorkingList(null);
        }
        List<Skill> skillList=getActualSkill(applicantId);
        List<Language> languagesList=getActualLanguage(applicantId);
        List<PreferredFunction> functionList=getExpectedFunction(applicantId);
        List<PreferredIndustry> industryList=getExpectedIndustry(applicantId);
        List<PreferredLocation> locationList=getExpectedLocation(applicantId);
        resumeInfo.setApplicant(applicant);
        resumeInfo.setSkillList(skillList);
        resumeInfo.setLanguageList(languagesList);
        resumeInfo.setPreFunctionList(functionList);
        resumeInfo.setPreIndustryList(industryList);
        resumeInfo.setPreLocationList(locationList);
        return resumeInfo;
    }

    //改变投递的简历的状态
    public int changeRecruitState(int state,int resumeId){
        Application app=enterpriseDao.get(Application.class,resumeId);
        app.setApplicationState(state);
        enterpriseDao.update(app);
        return app.getApplicationState();
    }


    //关闭招聘信息
    public boolean closeRecruit(int jobId){
        Recruitment ru=enterpriseDao.get(Recruitment.class,jobId);
        Byte st=2;
        ru.setState(st);
        enterpriseDao.update(ru);
        if(ru.getState()==2) return true;
        else return false;
    }

    public boolean openJob(int jobId){
        Recruitment recruitment=enterpriseDao.get(Recruitment.class,jobId);
        Byte st=1;
        recruitment.setState(st);
        enterpriseDao.update(recruitment);
        return true;
    }

    //注册公司
    public User getCompanyReg(String account,String password,String name){
        User user=new User();
        user.setUserName(account);
        user.setPassword(password);
        user.setRole(2);
        enterpriseDao.save(user);
        int user_id=enterpriseDao.getUserId(account,password);
        User user1=enterpriseDao.get(User.class,user_id);
        if(user1!=null){
            Enterprise enterprise=new Enterprise();
            enterprise.setEnterpriseId(user_id);
            enterprise.setName(name);
            enterpriseDao.save(enterprise);
        }
        return user1;
    }

    public Enterprise enterprise(int id){
        Enterprise enterprise = DataUtil.getEnterprise(id);
        if(enterprise==null)return null;
        enterprise.setActualType(DataUtil.getDictionary(enterprise.getType()).getDictionaryName());
        enterprise.setActualScale(DataUtil.getDictionary(enterprise.getScale()).getDictionaryName());
        enterprise.setActualIndustry(DataUtil.getDictionary(enterprise.getIndustry()).getDictionaryName());
        enterprise.setActualDomicile(DataUtil.getDictionary(enterprise.getDomicile()).getDictionaryName());
        return enterprise;
    }

    //获得某个求职者的最高学历
    public String getHighestDegree(int applicantId){
        List<Experience> exList=enterpriseDao.getExperienceList(applicantId);
        List<EducationExperience> eduList=getActualStudyExperience(exList);
        int highDegree=1047;
        int i=0;
        while(i<eduList.size()) {
            int temp=eduList.get(i).getDegree();
            if(temp > highDegree) highDegree=temp;
            i++;
        }
        String degree=getActualAttribute(highDegree,9);
        return degree;
    }

    //获得包括实际信息的applicant类
    public Applicant getActualInfoApplicant(int applicantId){
        Applicant applicant=enterpriseDao.get(Applicant.class,applicantId);
        applicant.setActualGender(getPersonGender(applicant.getGender()));
        if(applicant.getBirthdate()!=null)applicant.setActualBirthdate(getActualTime(applicant.getBirthdate()));
        if(applicant.getDomicile()!=null)applicant.setActualDomicile(getActualAttribute(applicant.getDomicile(),1));
        if(applicant.getWorkingStatus()!=null)applicant.setActualWorkingStatus(getCurrentWorkStatus(applicant.getWorkingStatus()));
        if(applicant.getWorkType()!=null)applicant.setActualWorkType(getExpectedWorkType(applicant.getWorkType()));
        return applicant;
    }

    //获得某个求职者的工作经历列表
    public List<Experience> getSpecificExperience(int applicantId){
        List<Experience> list=enterpriseDao.getExperienceList(applicantId);
        if(list!=null) {
            for (int i = 0; i < list.size(); i++) {
                String start = getActualTime(list.get(i).getStartTime());
                String end = getActualTime(list.get(i).getEndTime());
                list.get(i).setActualStartTime(start);
                list.get(i).setActualEndTime(end);
            }
            return list;
        }
        else return null;
    }

    //查找某个求职者的学习经历
    public List<EducationExperience> getActualStudyExperience(List<Experience> expList){
        List<EducationExperience> eduList = new ArrayList<>();
        for(int i=0;i<expList.size();i++){
            int id=expList.get(i).getExperienceId();
            if(enterpriseDao.judgeEducation(id)) {
                EducationExperience edu;
                edu = enterpriseDao.get(EducationExperience.class, id);
                edu.setStartTime(expList.get(i).getActualStartTime());
                edu.setEndTime(expList.get(i).getActualEndTime());
                String stuType=getActualStuType(edu.getStudentType());
                edu.setActualStudentType(stuType);
                edu.setActualDegree(getActualAttribute(edu.getDegree(),9));
                eduList.add(edu);
            }
        }
        return eduList;
    }


    //查找某个求职者的工作经历
    public List<WorkingExperience> getActualWorkExperience(List<Experience> expList){
        List<WorkingExperience> workList= new ArrayList<>();
        WorkingExperience work;
        for(int i=0;i<expList.size();i++){
            int id=expList.get(i).getExperienceId();
            if(enterpriseDao.judgeWork(id)) {
                work = enterpriseDao.get(WorkingExperience.class, id);
                work.setStartTime(expList.get(i).getActualStartTime());
                work.setEndTime(expList.get(i).getActualEndTime());
                work.setActualEnterpriseType(getActualAttribute(work.getEnterpriseType(),4));
                work.setActualEnterpriseScale(getActualAttribute(work.getEnterpriseScale(),7));
                work.setActualFunction(getActualAttribute(work.getFunction(),3));
                work.setActualIndustry(getActualAttribute(work.getIndustry(),2));
                work.setActualWorkType(getExpectedWorkType(work.getWorkType()));
                workList.add(work);
            }
        }
        return workList;
    }

    //获得某个求职者的掌握的技能
    public List<Skill> getActualSkill(int userId){
        List<Skill> list=enterpriseDao.getSkillList(userId);
        if(!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                Byte level = list.get(i).getLevel();
                int skillId = list.get(i).getSkillName();
                list.get(i).setActualSkillName(getActualAttribute(skillId,5));
                list.get(i).setActualLevel(getActualSkillLevel(level));
            }
        }
        return list;
    }

    //获得某个求职者的语言能力
    public List<Language> getActualLanguage(int userId){
        List<Language> list=enterpriseDao.getLanguageList(userId);
        if(!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                Byte rw = list.get(i).getRwAbility();
                Byte x = list.get(i).getxAbility();
                int language = list.get(i).getLanguage();
                list.get(i).setActualLanguage(getActualAttribute(language,6));
                list.get(i).setActualRwAbility(getActualSkillLevel(rw));
                list.get(i).setActualXAbility(getActualSkillLevel(x));
            }
        }
        return list;
    }

    //获取某个求职者的期望行业
    public List<PreferredIndustry> getExpectedIndustry(int userId){
        List<PreferredIndustry> list=enterpriseDao.getExpectedIndustyList(userId);
        if(!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                int id = list.get(i).getIndustry();
                list.get(i).setActualIndustry(getActualAttribute(id,2));
            }
        }
        return list;
    }

    //获取某个求职者的期望职能
    public List<PreferredFunction> getExpectedFunction(int userId){
        List<PreferredFunction> list=enterpriseDao.getExpectedFunctionList(userId);
        if(!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                int id = list.get(i).getFunction();
                list.get(i).setActualFunction(getActualAttribute(id,3));
            }
        }
        return list;
    }

    //获取某个求职者的期望地点
    public List<PreferredLocation> getExpectedLocation(int userId){
        List<PreferredLocation> list=enterpriseDao.getExpectedLocationList(userId);
        if(!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                int id = list.get(i).getLoactionId();
                list.get(i).setActualLocation(getActualAttribute(id,1));
            }
        }
        return list;
    }

//    //获得实际地址、规模、企业类型、技能名称、语种、学历、角色、行业、职能
//    public String getActualAttribute(int dicId){
//        Dictionary dic=enterpriseDao.get(Dictionary.class,dicId);
//        String str=dic.getDictionaryName();
//        return str;
//    }

    public String getActualAttribute(int dicId,int type){
        List<Dictionary> dic=new ArrayList<>();
        String str=new String();
        switch (type){
            case 1:dic=DataUtil.allCities();break;
            case 2:dic=DataUtil.allIndystries();break;
            case 3:dic=DataUtil.allFuctions();break;
            case 4:dic=DataUtil.allEnterpriseType();break;
            case 5:dic=DataUtil.allSkills();break;
            case 6:dic=DataUtil.allLanguages();break;
            case 7:dic=DataUtil.allScales();break;
            case 9:dic=DataUtil.allDegrees();break;
        }
        for (int i = 0; i < dic.size(); i++) {
            if(dic.get(i).getDictionaryId()==dicId) str=dic.get(i).getDictionaryName();
        }
        System.out.println("str is "+str);
        return str;
    }

    //获得实际的岗位开放状态
    public String getRecruitState(int i){
        String state;
        if (i==1) state="开放";
        else state="关闭";
        return state;
    }

    //获得实际的岗位发布时间
    public String getRecruitTime(Timestamp time){
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr = sdf.format(time);
        return timeStr;
    }

    //获得实际的时间
    public String getActualTime(Timestamp time){
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String timeStr = sdf.format(time);
        return timeStr;
    }

    //获得经验的时间区段
    public String getExperienceTime(Timestamp start,Timestamp end){
        String et=getActualTime(start)+getActualTime(end);
        return et;
    }

    //获得实际的性别
    public String getPersonGender(byte i){
        String gender;
        if (i==1) gender="男";
        else gender="女";
        return gender;
    }

    //获得实际学生类型
    public String getActualStuType(int i){
        String type;
        if (i==1) type="统招";
        else type="非统招";
        return type;
    }

    //获得当前工作状态
    public String getCurrentWorkStatus(int state){
        String status;
        switch (state){
            case 1: status="离岗"; break;
            case 2: status="在岗"; break;
            default: status="在岗 & 有换岗需求";
        }
        return status;
    }

    //获得实际期望的工作类型
    public String getExpectedWorkType(int wt){
        String ewt;
        switch (wt){
            case 1: ewt="全职"; break;
            case 2: ewt="兼职"; break;
            default: ewt="实习";
        }
        return ewt;
    }

    //获得审核状态
    public String getActualWorkState(int ws){
        String aws;
        switch (ws){
            case 1: aws="审核中"; break;
            case 2: aws="已录用"; break;
            default: aws="未录用";
        }
        return aws;
    }

    //获得技能的实际掌握程度
    public String getActualSkillLevel(Byte sl){
        String asl;
        switch (sl){
            case 1: asl="一般"; break;
            case 2: asl="良好"; break;
            case 3:asl="熟练";break;
            default: asl="精通";
        }
        return asl;
    }

    //保存修改企业信息
    public boolean saveOrupdateInfo(String str_enterprise,int e_id) throws ParseException {
        Enterprise enterprise = (Enterprise) jsonTojava(str_enterprise,Enterprise.class);

        enterprise.setIndustry(getIntIndustry(enterprise.getActualIndustry()));
        enterprise.setScale(getIntEnterpriseScale(enterprise.getActualScale()));
        enterprise.setDomicile(getIntDomicile(enterprise.getActualDomicile()));
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        enterprise.setFoundingTime(new Timestamp(format.parse(enterprise.getActualFoundingTime()).getTime()));
        enterprise.setType(getIntEnterpriseType(enterprise.getActualType()));
        enterprise.setEnterpriseId(e_id);
        enterpriseDao.update(enterprise);

        Enterprise saved_enterprise = enterpriseDao.get(Enterprise.class,e_id);
        if (enterprise.equals(saved_enterprise)) return true;
        return false;


    }

    //保存修改招聘信息
    public boolean saveOrupdateRecruitment(String r) throws ParseException {
        Recruitment recruitment = (Recruitment) jsonTojava(r,Recruitment.class);
        int recruitment_id = recruitment.getRecruitmentId();

        recruitment.setFunction(getIntFunction(recruitment.getActualFunction()));
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        recruitment.setTime(new Timestamp(format.parse(recruitment.getActualTime()).getTime()));
        recruitment.setMinDegree(getIntDegree(recruitment.getActualMinDegree()));
        recruitment.setRecruitmentId(recruitment_id);

        enterpriseDao.update(recruitment);

        Recruitment sd_recruitment = enterpriseDao.get(Recruitment.class,recruitment_id);
        if (recruitment.equals(sd_recruitment))return true;
        return false;


    }

    public int getIntFunction(String fuc){
        List<Dictionary> fuctions = DataUtil.allFuctions();
        int fuction = getIntAttribute(fuc,fuctions);
        return fuction;

    }

    public int getIntIndustry(String industry){
        List<Dictionary> industries = DataUtil.allIndystries();
        int industryyy = getIntAttribute(industry,industries);
        return industryyy;
    }

    public int getIntDomicile(String domicile){
        List<Dictionary> domiciles = DataUtil.allCities();
        int doml = getIntAttribute(domicile,domiciles);
        return doml;
    }

    public int getIntDegree(String dg){
        List<Dictionary> degrees = DataUtil.allDegrees();
        int degree = getIntAttribute(dg,degrees);
        return degree;
    }

    //由String类型的属性获得int类型的属性
    public int getIntAttribute(String attribute,List<Dictionary> list){
        for (int i=0;i<list.size();i++){
            Dictionary dict = list.get(i);
            String attrbt = dict.getDictionaryName();
            if (attribute==attrbt) return dict.getDictionaryId();
        }
        return 0;
    }

    public int getIntEnterpriseScale(String escale){
        List<Dictionary> scales = DataUtil.allScales();
        int entscale = getIntAttribute(escale,scales);
        return  entscale;
    }

    public int getIntEnterpriseType(String etype){
        List<Dictionary> enterprisetypes = DataUtil.allEnterpriseType();
        int enterprisetype = getIntAttribute(etype,enterprisetypes);
        return enterprisetype;

    }

    public <T> Object jsonTojava(String str,Class<T> tClass){
        JSONObject jsonObject = JSON.parseObject(str);
        Object object = jsonObject.toJavaObject(tClass.getClass());
        return object;
    }
}
