package com._51job.spider;

import com._51job.domain.*;
import com._51job.domain.Dictionary;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZLZP implements PageProcessor,Runnable {
    private static CopyOnWriteArrayList<Recruitment> jobs;
    private static CopyOnWriteArrayList<Enterprise> enterprises;
    private static Configuration configuration;
    private static SessionFactory sessionFactory;
    private static Session session;
    private static Transaction transaction;
    private static ZLZP zlzp;
    private static String regCompany="http://company\\.zhaopin\\.com.*htm$";
    private static String regJob="^http://jobs.zhaopin.com.*htm$";
    private static Set<String> comName;

    private Site site=Site.me()
            .setDomain("http://www.zhaopin.com")
            .setRetryTimes(1)
            .setSleepTime(1000)
            .setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36")
            .setCharset("utf-8")
            .setTimeOut(5000);

    private ZLZP() {
        jobs=new CopyOnWriteArrayList<>();
        enterprises=new CopyOnWriteArrayList<>();
        configuration=new Configuration().configure("hibernate.cfg.xml");
        sessionFactory=configuration.buildSessionFactory();
        session=sessionFactory.openSession();
        transaction=session.beginTransaction();
        comName=new HashSet<>();
    }
    public Session getSession(){return sessionFactory.openSession();}

    public void process(Page page) {
        Html html=page.getHtml();
        List<String> links=html.links().regex(regCompany).all();
        page.addTargetRequests(links);
        if(page.getUrl().regex(regJob).match()){
//            String name=html.xpath("/html/body/div[5]/div[1]/div[1]/h1/text()").get();
//            System.out.println(name);
//            List<String> fulis=html.xpath("/html/body/div[5]/div[1]/div[1]/div[1]/span/text()").all();
//            List<String> desc=html.xpath("/html/body/div[6]/div[1]/div[1]/div/div[1]/p/text()").all();
//            String company=html.xpath("/html/body/div[5]/div[1]/div[1]/h2/a/text()").get();
//            String salary=html.xpath("/html/body/div[6]/div[1]/ul/li[1]/strong/text()").get();
//            String addr=html.xpath("/html/body/div[6]/div[1]/ul/li[2]/strong/a/text()").get();
//            String pubtime=html.xpath("/html/body/div[6]/div[1]/ul/li[3]/strong/span/text()").get();
//            String feature=html.xpath("/html/body/div[6]/div[1]/ul/li[4]/strong/text()").get();
//            String exper=html.xpath("/html/body/div[6]/div[1]/ul/li[5]/strong/text()").get();
//            String edu=html.xpath("/html/body/div[6]/div[1]/ul/li[6]/strong/text()").get();
//            String t_total=html.xpath("/html/body/div[6]/div[1]/ul/li[7]/strong/text()").get();
//            String function=html.xpath("/html/body/div[6]/div[1]/ul/li[8]/strong/a/text()").get();
//            Pattern pattern=Pattern.compile("[^(0-9)*]");
//            Matcher matcher=pattern.matcher(t_total);
//            t_total=matcher.replaceAll("").trim();
//            int total =Integer.parseInt(t_total);
//            Recruitment recruitment=new Recruitment();
//            recruitment.setPost(name);
//            recruitment.setDescription(desc.toString());
//            Pattern p = Pattern.compile("[0-9]");
//            Matcher m = p.matcher(salary);
//            if(m.find()){
//                int a=salary.indexOf("-");
//                int b=salary.indexOf("/");
//                int low=Integer.parseInt(salary.substring(0,a));
//                int high=Integer.parseInt(salary.substring(a+1,b-1));
//                int s_int=(low+high)/2;
//                recruitment.setSalary(s_int);
//            }else {recruitment.setSalary(0);}
//            Matcher matcher1=p.matcher(exper);
//            if(matcher1.find()){
//                int a=salary.indexOf("-");
//                int low=Integer.parseInt(exper.substring(0,a));
//                recruitment.setMinSeniority(low);
//            }else recruitment.setMinSeniority(0);
//            recruitment.setState((byte)1);
//            SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            if(pubtime.length()>10&&pubtime.startsWith("2")){
//                try {
//                    Date date=format.parse(pubtime);
//                    recruitment.setTime(new Timestamp(date.getTime()));
//                } catch (ParseException e) {
//                    e.printStackTrace();
//                }
//            }else{
//                recruitment.setTime(new Timestamp(new Date().getTime()));
//            }
//            switch (feature){
//                case "全职":
//                    recruitment.setType((byte)1);break;
//                case "兼职":
//                    recruitment.setType((byte)2);break;
//                case "实习":
//                    recruitment.setType((byte)3);break;
//            }
//            switch (edu){
//                case "高中":
//                    recruitment.setType((byte)2);break;
//                case "大专":
//                    recruitment.setType((byte)3);break;
//                case "本科":
//                    recruitment.setType((byte)4);break;
//                case "硕士":
//                    recruitment.setType((byte)5);break;
//                case "博士":
//                    recruitment.setType((byte)6);break;
//            }
//            jobs.add(recruitment);
        }else if(page.getUrl().regex(regCompany).match()){
            String name=html.xpath("/html/body/div[2]/div[1]/div[1]/h1/text()").get();
            if(!comName.contains(name)){
                comName.add(name);
                System.out.println(name);
                String type=html.xpath("/html/body/div[2]/div[1]/div[1]/table/tbody/tr[1]/td[2]/span/text()").get();
                int i_type_id=enterpriseType(type);
                String scale=html.xpath("/html/body/div[2]/div[1]/div[1]/table/tbody/tr[2]/td[2]/span/text()").get();
                int i_scale_id=enterPriseScale(scale);
                String industry=html.xpath("/html/body/div[2]/div[1]/div[1]/table/tbody/tr[4]/td[2]/span/text()").get();
                String[] inds=industry.split(",");
                int inds_id=industry(inds[0]);
                String addr=html.xpath("/html/body/div[2]/div[1]/div[1]/table/tbody/tr[5]/td[2]/span/text()").get();
                String desc=html.xpath("/html/body/div[2]/div[1]/div[2]/div/text()").get();
                desc+=delHTMLTag(html.xpath("/html/body/div[2]/div[1]/div[2]/div").get());
                Enterprise enterprise=new Enterprise();
                enterprise.setName(name);
                enterprise.setType(i_type_id);
                enterprise.setScale(i_scale_id);
                enterprise.setIndustry(inds_id);
                enterprise.setAddress(addr);
                enterprise.setDescription(desc);
                enterprise.setDomicile(80);
                enterprise.setFoundingTime(new Timestamp(new Date().getTime()));
                if(enterprise.getIndustry()!=0&&enterprise.getType()!=0)enterprises.add(enterprise);
            }

        }
    }

    public Site getSite() {
        return this.site;
    }

    public static void main(String[] args) {
        zlzp=new ZLZP();
        Query<Enterprise> query=zlzp.getSession().createQuery("from Enterprise",Enterprise.class);
        List<Enterprise> list=query.list();
        for(Enterprise enterprise:list){
            comName.add(enterprise.getName());
        }
        new Thread(zlzp).start();
        Timer timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println(new Date());
                for(Enterprise enterprise: enterprises){
                    session.save(enterprise);
                }
                enterprises.clear();
                transaction.commit();
                transaction=session.beginTransaction();
            }
        },3000,3000);
    }

    @Override
    public void run() {
        String[] urls=new String[90];
        for(int i=0;i<90;i++){
            urls[i]="http://sou.zhaopin.com/jobs/searchresult.ashx?jl=深圳&kw=c&isadv=0&sg=0390cfbf86794329aa5a271e7fb4029d&p="+(i+1);
        }
        Spider.create(zlzp)
                .addUrl(urls)
                .thread(20)
                .run();
    }
    private static int industry(String s_industry){
        Query<com._51job.domain.Dictionary> query=zlzp.getSession().createQuery("from Dictionary where type=2", com._51job.domain.Dictionary.class);
        for(Dictionary dictionary: query.list()){
            if(dictionary.getDictionaryName().equals(s_industry))return dictionary.getDictionaryId();
        }
        return 0;
    }
    public static String delHTMLTag(String htmlStr){
        String regEx_script="<script[^>]*?>[\\s\\S]*?<\\/script>"; //定义script的正则表达式
        String regEx_style="<style[^>]*?>[\\s\\S]*?<\\/style>"; //定义style的正则表达式
        String regEx_html="<[^>]+>"; //定义HTML标签的正则表达式
        String nbsp="&nbsp;";

        Pattern p_script=Pattern.compile(regEx_script,Pattern.CASE_INSENSITIVE);
        Matcher m_script=p_script.matcher(htmlStr);
        htmlStr=m_script.replaceAll(""); //过滤script标签

        Pattern p_style=Pattern.compile(regEx_style,Pattern.CASE_INSENSITIVE);
        Matcher m_style=p_style.matcher(htmlStr);
        htmlStr=m_style.replaceAll(""); //过滤style标签

        Pattern p_html=Pattern.compile(regEx_html,Pattern.CASE_INSENSITIVE);
        Matcher m_html=p_html.matcher(htmlStr);
        htmlStr=m_html.replaceAll(""); //过滤html标签

        Pattern nb_=Pattern.compile(nbsp,Pattern.CASE_INSENSITIVE);
        Matcher m_htm=nb_.matcher(htmlStr);
        htmlStr=m_htm.replaceAll(""); //过滤html标签

        return htmlStr.trim(); //返回文本字符串
    }
    private int enterpriseType(String s_type){
        Query<Dictionary> query=getSession().createQuery("from Dictionary where type=4",Dictionary.class);
        for (Dictionary dictionary:query.list()){
            if(dictionary.getDictionaryName().equals(s_type))return dictionary.getDictionaryId();
        }
        return 0;
    }

    private int enterPriseScale(String s_scale){
        Query<Dictionary> query=getSession().createQuery("from Dictionary where type=7",Dictionary.class);
        for (Dictionary dictionary:query.list()){
            if(dictionary.getDictionaryName().equals(s_scale))return dictionary.getDictionaryId();
        }
        return 0;
    }
}
