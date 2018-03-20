package com.android.launcher3.util.locale.hanzi;

import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import com.android.launcher3.util.locale.hanzi.HanziToPinyin.Token;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class HanziToMultiPinyin {
    private static final int INITIAL_CAPACITY = 100;
    private static final LinkedHashMap<String, String> multiPinyinMap = new LinkedHashMap(100);
    private static HanziToMultiPinyin sInstance;

    protected HanziToMultiPinyin() {
    }

    private static void putMultiPinyin(String ascii, String pinyins) {
        multiPinyinMap.put(ascii, pinyins);
    }

    private static void initMap129() {
        putMultiPinyin("129-64", "kao,qiao,yu");
        putMultiPinyin("129-79", "guan,kuang");
        putMultiPinyin("129-85", "yi,ji");
        putMultiPinyin("129-92", "cheng,sheng");
        putMultiPinyin("129-118", "zhi,luan");
        putMultiPinyin("129-133", "qi,zhai");
        putMultiPinyin("129-144", "wei,men");
        putMultiPinyin("129-151", "jin,san");
        putMultiPinyin("129-154", "tuo,cha,duo");
        putMultiPinyin("129-156", "gan,han");
        putMultiPinyin("129-159", "chang,zhang");
        putMultiPinyin("129-163", "chao,miao");
        putMultiPinyin("129-166", "yao,fo");
        putMultiPinyin("129-188", "che,ju");
        putMultiPinyin("129-193", "xian,xuan");
        putMultiPinyin("129-204", "yi,chi");
        putMultiPinyin("129-206", "han,gan");
        putMultiPinyin("129-212", "zhao,shao");
        putMultiPinyin("129-215", "zhan,dian");
        putMultiPinyin("129-221", "hui,huai");
    }

    private static void initMap130() {
        putMultiPinyin("130-68", "cheng,ting");
        putMultiPinyin("130-74", "shu,dou");
        putMultiPinyin("130-77", "tuo,tui");
        putMultiPinyin("130-101", "che,ju");
        putMultiPinyin("130-109", "chu,ti");
        putMultiPinyin("130-115", "hu,chi");
        putMultiPinyin("130-121", "cui,zu");
        putMultiPinyin("130-122", "liang,lia");
        putMultiPinyin("130-138", "liang,jing");
        putMultiPinyin("130-160", "cheng,chen");
        putMultiPinyin("130-163", "zhong,tong");
        putMultiPinyin("130-171", "tang,dang");
        putMultiPinyin("130-184", "chou,qiao");
        putMultiPinyin("130-198", "cai,si");
        putMultiPinyin("130-200", "ce,ze,zhai");
        putMultiPinyin("130-204", "zan,za,zBn");
        putMultiPinyin("130-208", "zhi,si");
        putMultiPinyin("130-215", "jiang,gou");
        putMultiPinyin("130-223", "qian,jian");
        putMultiPinyin("130-224", "que,jue");
        putMultiPinyin("130-225", "cang,chen");
        putMultiPinyin("130-232", "ta,tan");
        putMultiPinyin("130-243", "zao,cao");
        putMultiPinyin("130-247", "chuan,zhuan");
        putMultiPinyin("130-250", "qi,cou");
    }

    private static void initMap131() {
        putMultiPinyin("131-69", "lv,lou");
        putMultiPinyin("131-71", "piao,biao");
        putMultiPinyin("131-92", "cheng,deng");
        putMultiPinyin("131-93", "zhan,zhuan,chan");
        putMultiPinyin("131-99", "tie,jian");
        putMultiPinyin("131-101", "jiao,yao");
        putMultiPinyin("131-104", "yu,ju");
        putMultiPinyin("131-105", "e,wu");
        putMultiPinyin("131-114", "jia,jie");
        putMultiPinyin("131-123", "chan,tan,shan");
        putMultiPinyin("131-140", "ni,yi");
        putMultiPinyin("131-168", "rang,xiang");
        putMultiPinyin("131-170", "hui,xie");
        putMultiPinyin("131-181", "dui,rui,yue");
        putMultiPinyin("131-182", "dui,rui,yue");
        putMultiPinyin("131-184", "chang,zhang");
        putMultiPinyin("131-186", "er,ni");
        putMultiPinyin("131-202", "yu,shu");
        putMultiPinyin("131-220", "yin,you");
        putMultiPinyin("131-244", "jing,cheng");
    }

    private static void initMap132() {
        putMultiPinyin("132-74", "yi kB,yi kB no bo li,tB ko");
        putMultiPinyin("132-119", "kei,ke");
        putMultiPinyin("132-120", "cha,sha");
        putMultiPinyin("132-131", "bao,bo");
        putMultiPinyin("132-134", "du,zhuo");
        putMultiPinyin("132-140", "zhi,duan");
        putMultiPinyin("132-145", "da,zha");
        putMultiPinyin("132-150", "tuan,zhuan");
        putMultiPinyin("132-151", "lu,jiu");
        putMultiPinyin("132-164", "jiao,chao");
        putMultiPinyin("132-179", "jin,jing");
        putMultiPinyin("132-181", "xie,lie");
        putMultiPinyin("132-197", "jin,jing");
        putMultiPinyin("132-216", "weng,yang");
        putMultiPinyin("132-224", "jiao,chao");
        putMultiPinyin("132-231", "tong,dong");
        putMultiPinyin("132-240", "rang,xiang");
        putMultiPinyin("132-252", "tao,yao");
    }

    private static void initMap133() {
        putMultiPinyin("133-77", "zang,cang");
        putMultiPinyin("133-84", "kui,gui");
        putMultiPinyin("133-92", "ke,qia");
        putMultiPinyin("133-94", "qu,ou");
        putMultiPinyin("133-105", "shuai,lv");
        putMultiPinyin("133-106", "guan,kuang");
        putMultiPinyin("133-110", "yang,ang");
        putMultiPinyin("133-122", "zhe,zhai");
        putMultiPinyin("133-123", "han,an");
        putMultiPinyin("133-126", "zha,zhai");
        putMultiPinyin("133-132", "zhi,shi");
        putMultiPinyin("133-149", "qian,lan");
        putMultiPinyin("133-154", "gong,hong");
        putMultiPinyin("133-155", "lin,miao");
        putMultiPinyin("133-156", "rou,qiu");
        putMultiPinyin("133-162", "can,shen,cen,san");
        putMultiPinyin("133-163", "can,shen,cen,san");
        putMultiPinyin("133-164", "can,shen,cen,san");
        putMultiPinyin("133-165", "ai,yi");
        putMultiPinyin("133-172", "zhuo,yi,li,jue");
        putMultiPinyin("133-176", "wei,yu");
        putMultiPinyin("133-186", "xuan,song");
        putMultiPinyin("133-188", "dou,cun");
        putMultiPinyin("133-195", "jie,ge");
        putMultiPinyin("133-197", "e,hua");
        putMultiPinyin("133-200", "na,ne");
        putMultiPinyin("133-201", "xue,chuo,jue");
        putMultiPinyin("133-202", "dou,ru");
        putMultiPinyin("133-203", "hou,hong,ou");
        putMultiPinyin("133-215", "tun,tian");
        putMultiPinyin("133-216", "hua,qi");
        putMultiPinyin("133-221", "wen,min");
        putMultiPinyin("133-222", "mou,m");
        putMultiPinyin("133-227", "tie,che");
        putMultiPinyin("133-230", "qi,zhi");
        putMultiPinyin("133-232", "zi,ci");
        putMultiPinyin("133-233", "xu,hou,gou");
        putMultiPinyin("133-235", "xiao,hao");
        putMultiPinyin("133-236", "da,dan");
        putMultiPinyin("133-238", "xian,gan");
    }

    private static void initMap134() {
        putMultiPinyin("134-65", "die,xi");
        putMultiPinyin("134-67", "shu,xun");
        putMultiPinyin("134-70", "huai,shi");
        putMultiPinyin("134-72", "e,an");
        putMultiPinyin("134-74", "wai,he,wo,wa,gua,guo");
        putMultiPinyin("134-79", "die,di");
        putMultiPinyin("134-84", "yuan,yun");
        putMultiPinyin("134-92", "po,ba,bo");
        putMultiPinyin("134-93", "liang,lang");
        putMultiPinyin("134-96", "lie,lv");
        putMultiPinyin("134-100", "chuo,yue");
        putMultiPinyin("134-104", "bei,bai");
        putMultiPinyin("134-107", "jia,qian");
        putMultiPinyin("134-109", "dan,xian,yan");
        putMultiPinyin("134-121", "gong,hong");
        putMultiPinyin("134-126", "sha,shB");
        putMultiPinyin("134-129", "wo,wei");
        putMultiPinyin("134-130", "jin,yin");
        putMultiPinyin("134-132", "er,wa");
        putMultiPinyin("134-135", "jie,ze,zuo");
        putMultiPinyin("134-143", "zhuo,zhao");
        putMultiPinyin("134-148", "zhun,tun,xiang,dui");
        putMultiPinyin("134-151", "sha,jie,die,ti");
        putMultiPinyin("134-156", "yue,wa");
        putMultiPinyin("134-157", "zi,ci");
        putMultiPinyin("134-158", "bi,tu");
        putMultiPinyin("134-166", "qing,ying");
        putMultiPinyin("134-168", "ze,shi");
        putMultiPinyin("134-174", "tan,chan,tuo");
        putMultiPinyin("134-184", "huo,guo,xu");
        putMultiPinyin("134-185", "zan,za,zBn");
        putMultiPinyin("134-191", "huan,yuan,xuan,he");
        putMultiPinyin("134-193", "zhong,chuang");
        putMultiPinyin("134-198", "duo,zha");
        putMultiPinyin("134-204", "qiao,jiao");
        putMultiPinyin("134-206", "dan,shan,chan");
        putMultiPinyin("134-207", "pen,ben");
        putMultiPinyin("134-208", "can,sun,qi");
        putMultiPinyin("134-215", "zao,qiao");
        putMultiPinyin("134-219", "he,xiao,hu");
        putMultiPinyin("134-223", "gong,hong");
        putMultiPinyin("134-225", "ma,mB");
        putMultiPinyin("134-229", "wa,gu");
        putMultiPinyin("134-231", "beng,pang");
        putMultiPinyin("134-233", "xian,qian,qie");
        putMultiPinyin("134-247", "zui,sui");
        putMultiPinyin("134-248", "zhe,zhu");
        putMultiPinyin("134-253", "jiao,dao");
        putMultiPinyin("134-254", "kai,ge");
    }

    private static void initMap135() {
        putMultiPinyin("135-65", "shan,can");
        putMultiPinyin("135-69", "xiao,jiao");
        putMultiPinyin("135-78", "de,dei");
        putMultiPinyin("135-96", "fu,?");
        putMultiPinyin("135-98", "chu,xu,shou");
        putMultiPinyin("135-99", "tan,chan");
        putMultiPinyin("135-100", "dan,tan");
        putMultiPinyin("135-104", "fan,bo");
        putMultiPinyin("135-109", "cu,za,he");
        putMultiPinyin("135-112", "tun,kuo");
        putMultiPinyin("135-117", "xu,shi");
        putMultiPinyin("135-128", "zhou,zhuo");
        putMultiPinyin("135-130", "yue,hui");
        putMultiPinyin("135-132", "jiao,qiao,chi");
        putMultiPinyin("135-140", "xin,hen");
        putMultiPinyin("135-151", "huo,o");
        putMultiPinyin("135-152", "he,xia");
        putMultiPinyin("135-155", "xiu,pi");
        putMultiPinyin("135-156", "zhou,chou");
        putMultiPinyin("135-157", "ji,jie,zhai");
        putMultiPinyin("135-165", "bo,pao,bao");
        putMultiPinyin("135-170", "mei,me,mo");
        putMultiPinyin("135-175", "xie,hai");
        putMultiPinyin("135-212", "za,zan,can");
        putMultiPinyin("135-217", "yan,za,nie");
        putMultiPinyin("135-236", "wei,tong");
        putMultiPinyin("135-249", "chuan,chui");
        putMultiPinyin("135-252", "quan,juan");
    }

    private static void initMap136() {
        putMultiPinyin("136-83", "yu,zhun");
        putMultiPinyin("136-84", "qian,su");
        putMultiPinyin("136-98", "mei,fen");
        putMultiPinyin("136-99", "rong,keng");
        putMultiPinyin("136-100", "tun,dun");
        putMultiPinyin("136-101", "ben,fen");
        putMultiPinyin("136-104", "di,lan");
        putMultiPinyin("136-109", "pan,ban");
        putMultiPinyin("136-161", "pou,fu");
        putMultiPinyin("136-169", "beng,feng");
        putMultiPinyin("136-170", "di,fang");
        putMultiPinyin("136-171", "que,jue");
        putMultiPinyin("136-174", "xia,jia");
        putMultiPinyin("136-178", "qin,jin");
        putMultiPinyin("136-188", "sui,su");
        putMultiPinyin("136-189", "qing,zheng");
        putMultiPinyin("136-193", "zheng,cheng");
        putMultiPinyin("136-195", "chong,shang");
        putMultiPinyin("136-199", "chu,tou");
        putMultiPinyin("136-201", "xian,kan");
        putMultiPinyin("136-203", "yi,shi");
        putMultiPinyin("136-233", "mao,mou,wu");
        putMultiPinyin("136-236", "e,ai,ye");
    }

    private static void initMap137() {
        putMultiPinyin("137-112", "ou,qiu");
        putMultiPinyin("137-113", "qian,zan,jian");
        putMultiPinyin("137-116", "zhuan,tuan");
        putMultiPinyin("137-121", "zhi,di");
        putMultiPinyin("137-126", "zhi,zhuo");
        putMultiPinyin("137-145", "kuai,tui");
        putMultiPinyin("137-146", "tuan,dong");
        putMultiPinyin("137-148", "qiao,que");
        putMultiPinyin("137-150", "zun,dun");
        putMultiPinyin("137-153", "duo,hui");
        putMultiPinyin("137-167", "qiao,ao");
        putMultiPinyin("137-169", "yi,tu");
        putMultiPinyin("137-174", "xue,bo,jue");
        putMultiPinyin("137-183", "xian,lan");
        putMultiPinyin("137-224", "gu,ying");
        putMultiPinyin("137-225", "jiang,xiang");
        putMultiPinyin("137-226", "feng,pang");
        putMultiPinyin("137-233", "xiong,xuan");
        putMultiPinyin("137-237", "yuan,wan");
        putMultiPinyin("137-238", "mao,wan");
        putMultiPinyin("137-250", "tao,ben");
        putMultiPinyin("137-252", "yan,tao");
    }

    private static void initMap138() {
        putMultiPinyin("138-65", "jia,ga,xia");
        putMultiPinyin("138-68", "di,ti");
        putMultiPinyin("138-73", "quan,juan");
        putMultiPinyin("138-76", "she,chi,zha");
        putMultiPinyin("138-81", "xun,zhui");
        putMultiPinyin("138-85", "po,ha,tai");
        putMultiPinyin("138-87", "ao,yu");
        putMultiPinyin("138-98", "duo,che");
        putMultiPinyin("138-99", "ding,tian");
        putMultiPinyin("138-109", "fu,you");
        putMultiPinyin("138-111", "hai,jie");
        putMultiPinyin("138-115", "keng,hang");
        putMultiPinyin("138-118", "fou,pei,pi");
        putMultiPinyin("138-120", "yue,jue");
        putMultiPinyin("138-123", "na,nan");
        putMultiPinyin("138-128", "wan,yuan");
        putMultiPinyin("138-133", "ni,nai");
        putMultiPinyin("138-136", "xian,xuan,xu");
        putMultiPinyin("138-137", "zhi,yi");
        putMultiPinyin("138-163", "hua,huo");
        putMultiPinyin("138-165", "gou,du");
        putMultiPinyin("138-174", "jun,xun");
        putMultiPinyin("138-175", "kua,hu");
        putMultiPinyin("138-183", "shen,xian");
        putMultiPinyin("138-191", "cheng,sheng");
        putMultiPinyin("138-195", "wu,mu");
        putMultiPinyin("138-197", "chuo,lai");
        putMultiPinyin("138-203", "pou,bi");
        putMultiPinyin("138-204", "nei,sui");
        putMultiPinyin("138-211", "wu,yu");
        putMultiPinyin("138-214", "xi,ai");
        putMultiPinyin("138-220", "zhui,shui");
        putMultiPinyin("138-226", "ai,e");
        putMultiPinyin("138-231", "pou,pei,bu");
        putMultiPinyin("138-239", "fu,fan");
        putMultiPinyin("138-247", "quan,juan");
        putMultiPinyin("138-250", "qian,jin");
        putMultiPinyin("138-254", "wan,wa");
    }

    private static void initMap139() {
        putMultiPinyin("139-66", "zhou,chou");
        putMultiPinyin("139-67", "chuo,nao");
        putMultiPinyin("139-70", "an,n<e");
        putMultiPinyin("139-71", "hun,kun");
        putMultiPinyin("139-78", "huB,dB tBi");
        putMultiPinyin("139-80", "dang,yang");
        putMultiPinyin("139-83", "ruo,chuo");
        putMultiPinyin("139-85", "tou,yu");
        putMultiPinyin("139-88", "di,ti");
        putMultiPinyin("139-92", "ruan,nen");
        putMultiPinyin("139-102", "yi,pei");
        putMultiPinyin("139-115", "tuo,duo");
        putMultiPinyin("139-129", "tou,yu");
        putMultiPinyin("139-131", "chu,zou");
        putMultiPinyin("139-139", "ao,yun,wo");
        putMultiPinyin("139-142", "qin,shen");
        putMultiPinyin("139-145", "jie,suo");
        putMultiPinyin("139-169", "han,nan");
        putMultiPinyin("139-205", "xian,yan,jin");
        putMultiPinyin("139-214", "huan,xuan,qiong");
        putMultiPinyin("139-252", "xian,qian");
    }

    private static void initMap140() {
        putMultiPinyin("140-64", "xie,hui");
        putMultiPinyin("140-65", "huan,quan");
        putMultiPinyin("140-70", "zhu,chuo");
        putMultiPinyin("140-73", "zi,ma");
        putMultiPinyin("140-79", "sun,xun");
        putMultiPinyin("140-95", "tu,jia");
        putMultiPinyin("140-112", "bao,shi");
        putMultiPinyin("140-131", "jin,qin");
        putMultiPinyin("140-138", "ju,lou");
        putMultiPinyin("140-156", "l<e,luo");
        putMultiPinyin("140-161", "kei,ke");
        putMultiPinyin("140-165", "shu,zhu");
        putMultiPinyin("140-168", "jie,ji");
        putMultiPinyin("140-180", "long,mang,meng,pang");
        putMultiPinyin("140-206", "ping,bing");
        putMultiPinyin("140-209", "xie,ti");
        putMultiPinyin("140-217", "shu,zhu");
        putMultiPinyin("140-219", "ni,ji");
        putMultiPinyin("140-226", "hong,long");
        putMultiPinyin("140-229", "han,an");
    }

    private static void initMap141() {
        putMultiPinyin("141-75", "zBi,ze mo");
        putMultiPinyin("141-81", "ke,ba");
        putMultiPinyin("141-84", "fu,nie");
        putMultiPinyin("141-108", "xie,ye");
        putMultiPinyin("141-140", "zu,cui");
        putMultiPinyin("141-154", "pi,bi");
        putMultiPinyin("141-164", "yang,dang");
        putMultiPinyin("141-170", "zhi,shi");
        putMultiPinyin("141-171", "shi,die");
        putMultiPinyin("141-174", "kan,zhan");
        putMultiPinyin("141-182", "wu,mao");
        putMultiPinyin("141-186", "ke,jie");
        putMultiPinyin("141-201", "dang,tang");
        putMultiPinyin("141-202", "rong,ying");
        putMultiPinyin("141-204", "kai,ai");
        putMultiPinyin("141-207", "kao,qiao");
        putMultiPinyin("141-210", "qin,qian");
        putMultiPinyin("141-222", "die,di");
        putMultiPinyin("141-227", "zhan,chan");
        putMultiPinyin("141-228", "zhan,chan");
        putMultiPinyin("141-239", "pi,pei");
        putMultiPinyin("141-254", "jiao,qiao");
    }

    private static void initMap142() {
        putMultiPinyin("142-64", "jue,gui");
        putMultiPinyin("142-69", "zhan,shan");
        putMultiPinyin("142-79", "xie,jie");
        putMultiPinyin("142-80", "ke,jie");
        putMultiPinyin("142-81", "gui,xi,juan");
        putMultiPinyin("142-95", "li,lie");
        putMultiPinyin("142-96", "gui,xi,juan");
        putMultiPinyin("142-99", "ying,hong");
        putMultiPinyin("142-121", "jing,xing");
        putMultiPinyin("142-146", "mo,wa");
        putMultiPinyin("142-165", "jian,san");
        putMultiPinyin("142-168", "sha,qie");
        putMultiPinyin("142-169", "qi,ji");
        putMultiPinyin("142-187", "shan,qiao,shen");
        putMultiPinyin("142-206", "chou,dao");
        putMultiPinyin("142-219", "me,mo");
        putMultiPinyin("142-221", "dun,tun");
        putMultiPinyin("142-223", "bai,ting");
        putMultiPinyin("142-237", "mang,meng,pang");
        putMultiPinyin("142-240", "bing,ping");
        putMultiPinyin("142-243", "ji,cuo");
    }

    private static void initMap143() {
        putMultiPinyin("143-64", "gui,wei,hui");
        putMultiPinyin("143-66", "sha,xia");
        putMultiPinyin("143-90", "qiang,se");
        putMultiPinyin("143-103", "po,pai");
        putMultiPinyin("143-123", "jue,zhang");
        putMultiPinyin("143-131", "juan,quan");
        putMultiPinyin("143-133", "xuan,yuan");
        putMultiPinyin("143-138", "qiang,jiang");
        putMultiPinyin("143-142", "dan,tan");
        putMultiPinyin("143-151", "dan,tan");
        putMultiPinyin("143-153", "qiang,jiang");
        putMultiPinyin("143-176", "zhuo,bo");
        putMultiPinyin("143-177", "tuo,yi");
        putMultiPinyin("143-186", "wang,jia,wa");
        putMultiPinyin("143-187", "cheng,zheng");
        putMultiPinyin("143-196", "cong,zong");
        putMultiPinyin("143-202", "shi,ti");
        putMultiPinyin("143-203", "jia,xia");
        putMultiPinyin("143-211", "ti,chi");
        putMultiPinyin("143-213", "zhi,zheng");
        putMultiPinyin("143-215", "zhong,chong");
        putMultiPinyin("143-217", "jiao,yao");
        putMultiPinyin("143-223", "qu,ju");
        putMultiPinyin("143-226", "ding,ting");
        putMultiPinyin("143-229", "gan,han");
        putMultiPinyin("143-230", "yi,qi");
        putMultiPinyin("143-231", "shi,tai");
        putMultiPinyin("143-232", "xi,lie");
        putMultiPinyin("143-235", "min,wen");
        putMultiPinyin("143-236", "min,wen");
        putMultiPinyin("143-243", "yu,shu");
        putMultiPinyin("143-244", "qi,shi");
        putMultiPinyin("143-247", "tun,zhun,dun");
        putMultiPinyin("143-248", "qian,qin");
        putMultiPinyin("143-251", "kuang,wang");
        putMultiPinyin("143-253", "kang,hang");
    }

    private static void initMap144() {
        putMultiPinyin("144-66", "min,men");
        putMultiPinyin("144-68", "kou,ju");
        putMultiPinyin("144-70", "nao,niu");
        putMultiPinyin("144-71", "tie,zhan");
        putMultiPinyin("144-72", "hu,gu");
        putMultiPinyin("144-73", "cu,ju,zu");
        putMultiPinyin("144-74", "you,chou");
        putMultiPinyin("144-76", "tu,die");
        putMultiPinyin("144-81", "you,yao");
        putMultiPinyin("144-86", "xu,xue");
        putMultiPinyin("144-87", "bi,pi");
        putMultiPinyin("144-89", "xi,shu");
        putMultiPinyin("144-102", "tiao,yao");
        putMultiPinyin("144-106", "xi,qi,xu");
        putMultiPinyin("144-107", "xiao,jiao");
        putMultiPinyin("144-109", "hu,kua");
        putMultiPinyin("144-119", "quan,zhuan");
        putMultiPinyin("144-128", "yuan,juan");
        putMultiPinyin("144-131", "yu,shu");
        putMultiPinyin("144-133", "jie,ke");
        putMultiPinyin("144-136", "hao,jiao");
        putMultiPinyin("144-142", "man,men");
        putMultiPinyin("144-150", "yi,nian");
        putMultiPinyin("144-173", "yuan,wan");
        putMultiPinyin("144-176", "lan,lin");
        putMultiPinyin("144-177", "yu,xu");
        putMultiPinyin("144-179", "juan,quan");
        putMultiPinyin("144-180", "tan,dan");
        putMultiPinyin("144-183", "chuo,chui");
        putMultiPinyin("144-184", "hun,men");
        putMultiPinyin("144-186", "e,wu");
        putMultiPinyin("144-187", "suo,rui");
        putMultiPinyin("144-201", "ti,shi");
        putMultiPinyin("144-212", "qi,kai");
        putMultiPinyin("144-213", "dang,shang,tang,yang");
        putMultiPinyin("144-215", "chen,xin,dan");
        putMultiPinyin("144-217", "ke,qia");
        putMultiPinyin("144-223", "cong,song");
        putMultiPinyin("144-224", "sai,si");
        putMultiPinyin("144-227", "gong,hong");
        putMultiPinyin("144-229", "su,shuo");
        putMultiPinyin("144-247", "kai,xi");
        putMultiPinyin("144-248", "xi,xie");
        putMultiPinyin("144-253", "cao,sao");
    }

    private static void initMap145() {
        putMultiPinyin("145-65", "xu,chu");
        putMultiPinyin("145-69", "gong,hong");
        putMultiPinyin("145-70", "cao,cong");
        putMultiPinyin("145-91", "qin,jin");
        putMultiPinyin("145-100", "di,chi");
        putMultiPinyin("145-101", "zhi,zhe");
        putMultiPinyin("145-102", "lou,lv");
        putMultiPinyin("145-126", "cheng,deng,zheng");
        putMultiPinyin("145-132", "dan,da");
        putMultiPinyin("145-135", "dui,dun,tun");
        putMultiPinyin("145-139", "xiao,jiao");
        putMultiPinyin("145-157", "nao,nang");
        putMultiPinyin("145-162", "jiao,ji");
        putMultiPinyin("145-164", "xuan,huan");
        putMultiPinyin("145-168", "cao,sao");
        putMultiPinyin("145-185", "ai,yi,ni");
        putMultiPinyin("145-188", "qi,ji");
        putMultiPinyin("145-190", "lan,xian");
        putMultiPinyin("145-222", "gang,zhuang");
        putMultiPinyin("145-223", "gang,zhuang");
        putMultiPinyin("145-239", "xi,hu");
        putMultiPinyin("145-241", "xi,hu");
        putMultiPinyin("145-242", "xi,hu");
        putMultiPinyin("145-251", "shang,jiong");
    }

    private static void initMap146() {
        putMultiPinyin("146-65", "le,li,cai");
        putMultiPinyin("146-66", "fan,fu");
        putMultiPinyin("146-70", "diao,di,yue,li");
        putMultiPinyin("146-71", "yu,wu");
        putMultiPinyin("146-72", "yu,wu,ku");
        putMultiPinyin("146-76", "tuo,chi,yi");
        putMultiPinyin("146-77", "gu,xi,ge,jie");
        putMultiPinyin("146-81", "xi,cha,qi");
        putMultiPinyin("146-82", "qian,qin");
        putMultiPinyin("146-85", "ba,ao");
        putMultiPinyin("146-86", "xi,zhe");
        putMultiPinyin("146-88", "zhi,sun,kan");
        putMultiPinyin("146-91", "kuang,wang,zai");
        putMultiPinyin("146-95", "hu,gu");
        putMultiPinyin("146-98", "dan,shen");
        putMultiPinyin("146-102", "ne,ni,rui,na");
        putMultiPinyin("146-104", "pou,fu");
        putMultiPinyin("146-106", "ao,niu");
        putMultiPinyin("146-107", "ze,zhBi");
        putMultiPinyin("146-110", "zhi,zhai");
        putMultiPinyin("146-112", "bu,pu");
        putMultiPinyin("146-113", "yao,tao");
        putMultiPinyin("146-117", "he,qia");
        putMultiPinyin("146-121", "pi,pei");
        putMultiPinyin("146-126", "jia,ya");
        putMultiPinyin("146-142", "cun,zun");
        putMultiPinyin("146-143", "yi,chi,hai");
        putMultiPinyin("146-145", "ce,se,chuo");
        putMultiPinyin("146-149", "kuo,guang");
        putMultiPinyin("146-157", "ru,na");
        putMultiPinyin("146-161", "die,she");
        putMultiPinyin("146-163", "lie ri");
        putMultiPinyin("146-168", "tuo,shui");
        putMultiPinyin("146-173", "suo,sB,shB");
        putMultiPinyin("146-174", "keng,qian");
        putMultiPinyin("146-178", "bang,peng");
        putMultiPinyin("146-182", "xie,jia");
        putMultiPinyin("146-185", "jiao,ku");
        putMultiPinyin("146-187", "huo,chi");
        putMultiPinyin("146-188", "tu,shu,cha");
        putMultiPinyin("146-189", "pou,fu");
        putMultiPinyin("146-191", "shu,song,sou");
        putMultiPinyin("146-192", "ye,yu");
        putMultiPinyin("146-193", "jue,zhuo");
        putMultiPinyin("146-195", "bu,pu,zhi");
        putMultiPinyin("146-201", "tuo,shui");
        putMultiPinyin("146-204", "wan,yu");
        putMultiPinyin("146-209", "fu,bu");
        putMultiPinyin("146-211", "wo,luo");
        putMultiPinyin("146-212", "juan,quan");
        putMultiPinyin("146-218", "ruo,wei,re");
        putMultiPinyin("146-220", "wo,xia");
        putMultiPinyin("146-225", "qing,qian");
        putMultiPinyin("146-231", "qian,wan");
        putMultiPinyin("146-237", "ni,nie,yi");
        putMultiPinyin("146-238", "huo,xu");
        putMultiPinyin("146-239", "shan,yan");
        putMultiPinyin("146-240", "zheng,ding");
        putMultiPinyin("146-244", "zou,zhou,chou");
    }

    private static void initMap147() {
        putMultiPinyin("147-64", "zheng,keng");
        putMultiPinyin("147-65", "jiu,you");
        putMultiPinyin("147-70", "pi,che");
        putMultiPinyin("147-72", "sai,zong,cai");
        putMultiPinyin("147-75", "zong,song");
        putMultiPinyin("147-78", "huang,yong");
        putMultiPinyin("147-83", "zan,zuan");
        putMultiPinyin("147-84", "xu,ju");
        putMultiPinyin("147-85", "ke,qia");
        putMultiPinyin("147-87", "ti,di");
        putMultiPinyin("147-95", "chong,dong");
        putMultiPinyin("147-98", "qian,jian");
        putMultiPinyin("147-111", "chou,zou");
        putMultiPinyin("147-114", "rong,nang");
        putMultiPinyin("147-115", "bang,peng");
        putMultiPinyin("147-120", "nu,nuo,nou");
        putMultiPinyin("147-121", "la,xie,xian");
        putMultiPinyin("147-131", "jie,zhe");
        putMultiPinyin("147-132", "pan,ban,po");
        putMultiPinyin("147-135", "hu,ku");
        putMultiPinyin("147-136", "zhi,nai");
        putMultiPinyin("147-140", "qiang,cheng");
        putMultiPinyin("147-141", "tian,shen");
        putMultiPinyin("147-144", "na,nuo");
        putMultiPinyin("147-151", "sa,sha,shai");
        putMultiPinyin("147-152", "chan,sun");
        putMultiPinyin("147-154", "jiu,liu,liao,jiao,nao");
        putMultiPinyin("147-158", "feng,peng");
        putMultiPinyin("147-159", "di,tu,zhi");
        putMultiPinyin("147-160", "qi,ji,cha");
        putMultiPinyin("147-161", "sou,song");
        putMultiPinyin("147-169", "gai,xi");
        putMultiPinyin("147-170", "hu,chu");
        putMultiPinyin("147-175", "zhi,nai");
        putMultiPinyin("147-176", "jiang,qiang");
        putMultiPinyin("147-179", "ao,qiao");
        putMultiPinyin("147-181", "nie,che");
        putMultiPinyin("147-183", "chan,can");
        putMultiPinyin("147-186", "se,mi,su");
        putMultiPinyin("147-188", "jiao,chao");
        putMultiPinyin("147-189", "chan,xian,can,shan");
        putMultiPinyin("147-190", "keng,qian");
        putMultiPinyin("147-203", "zan,zen,qian");
        putMultiPinyin("147-209", "heng,guang");
        putMultiPinyin("147-213", "zheng,cheng");
        putMultiPinyin("147-214", "hui,wei");
        putMultiPinyin("147-219", "dan,shan");
        putMultiPinyin("147-223", "xiao,sou");
        putMultiPinyin("147-227", "wei,tuo");
        putMultiPinyin("147-234", "qiao,yao,ji");
        putMultiPinyin("147-235", "zhua,wo");
        putMultiPinyin("147-241", "ze,zhai");
        putMultiPinyin("147-247", "qing,jing");
        putMultiPinyin("147-252", "qia,jia,ye");
    }

    private static void initMap148() {
        putMultiPinyin("148-84", "zhi,jie");
        putMultiPinyin("148-89", "lie,la");
        putMultiPinyin("148-94", "li,luo,yue");
        putMultiPinyin("148-96", "ti,zhi,zhai");
        putMultiPinyin("148-99", "ca,sa");
        putMultiPinyin("148-104", "jun,pei");
        putMultiPinyin("148-105", "li,luo");
        putMultiPinyin("148-106", "la,lai");
        putMultiPinyin("148-109", "lu,luo");
        putMultiPinyin("148-115", "xian,jian");
        putMultiPinyin("148-122", "she,nie");
        putMultiPinyin("148-125", "mi,mo");
        putMultiPinyin("148-128", "zan,cuan");
        putMultiPinyin("148-131", "li,shai");
        putMultiPinyin("148-137", "li,luo");
        putMultiPinyin("148-140", "qi,yi,ji");
        putMultiPinyin("148-144", "gan,han");
        putMultiPinyin("148-150", "wu,mou");
        putMultiPinyin("148-153", "chu,shou");
        putMultiPinyin("148-154", "ge,guo,e");
        putMultiPinyin("148-159", "duo,dui");
        putMultiPinyin("148-163", "duo,dui");
        putMultiPinyin("148-166", "duo,que");
        putMultiPinyin("148-170", "qi,yi,ji");
        putMultiPinyin("148-172", "xiao,xue");
        putMultiPinyin("148-173", "duo,que");
        putMultiPinyin("148-177", "ai,zhu");
        putMultiPinyin("148-178", "ai,zhu");
        putMultiPinyin("148-181", "shu,shuo");
        putMultiPinyin("148-184", "xiong,xuan");
        putMultiPinyin("148-189", "zhuo,zhu");
        putMultiPinyin("148-190", "yi,du");
        putMultiPinyin("148-193", "li,tai");
        putMultiPinyin("148-199", "jue,jiao");
        putMultiPinyin("148-203", "yu,zhong");
        putMultiPinyin("148-205", "wei,men");
        putMultiPinyin("148-211", "tou,tiao");
        putMultiPinyin("148-213", "yin,zhi");
        putMultiPinyin("148-226", "chan,jie");
        putMultiPinyin("148-229", "liu,you");
        putMultiPinyin("148-232", "pi,bi");
    }

    private static void initMap149() {
        putMultiPinyin("149-64", "tai,ying");
        putMultiPinyin("149-65", "di,de");
        putMultiPinyin("149-72", "tun,zhun");
        putMultiPinyin("149-105", "die,yi");
        putMultiPinyin("149-118", "xu,kua");
        putMultiPinyin("149-146", "qi,du");
        putMultiPinyin("149-149", "an,yan");
        putMultiPinyin("149-164", "shu,du");
        putMultiPinyin("149-169", "jian,lan");
        putMultiPinyin("149-251", "zeng,ceng");
        putMultiPinyin("149-252", "can,qian,jian");
        putMultiPinyin("149-254", "hui,kuai");
    }

    private static void initMap150() {
        putMultiPinyin("150-65", "qie,he");
        putMultiPinyin("150-67", "bi,pi");
        putMultiPinyin("150-68", "fen,ban");
        putMultiPinyin("150-70", "fei,ku");
        putMultiPinyin("150-72", "nv,ga");
        putMultiPinyin("150-75", "juan,zui");
        putMultiPinyin("150-77", "huang,mang,wang");
        putMultiPinyin("150-83", "tong,chuang");
        putMultiPinyin("150-88", "shu,zhu");
        putMultiPinyin("150-93", "dao,tiao,mu");
        putMultiPinyin("150-95", "qiu,gui");
        putMultiPinyin("150-102", "yu,wu");
        putMultiPinyin("150-107", "ren,er");
        putMultiPinyin("150-108", "tuo,zhe");
        putMultiPinyin("150-109", "di,duo");
        putMultiPinyin("150-113", "gu,gai");
        putMultiPinyin("150-115", "yi,li,duo,tuo");
        putMultiPinyin("150-121", "si,zhi,xi");
        putMultiPinyin("150-122", "yuan,wan");
        putMultiPinyin("150-123", "fei,bei");
        putMultiPinyin("150-128", "shu,dui");
        putMultiPinyin("150-131", "niu,chou");
        putMultiPinyin("150-134", "wo,yue");
        putMultiPinyin("150-138", "pi,mi");
        putMultiPinyin("150-144", "hu,di");
        putMultiPinyin("150-154", "di,duo");
        putMultiPinyin("150-156", "song,mB ti su");
        putMultiPinyin("150-158", "xian,zhen");
        putMultiPinyin("150-159", "si,tai");
        putMultiPinyin("150-162", "bao,fu");
        putMultiPinyin("150-164", "yi,xie");
        putMultiPinyin("150-170", "yi,duo,li");
        putMultiPinyin("150-171", "ni,chi");
        putMultiPinyin("150-174", "pan,ban");
        putMultiPinyin("150-179", "yang,ying");
        putMultiPinyin("150-187", "zhi,die");
        putMultiPinyin("150-188", "zha,zu");
        putMultiPinyin("150-191", "bu,pu");
        putMultiPinyin("150-194", "ba,fu,pei,bo,bie");
        putMultiPinyin("150-195", "duo,zuo,wu");
        putMultiPinyin("150-196", "bi,bie");
        putMultiPinyin("150-200", "bei,pei");
        putMultiPinyin("150-201", "shi,fei");
        putMultiPinyin("150-203", "cha,zha");
        putMultiPinyin("150-214", "qi,qie");
        putMultiPinyin("150-222", "ben,bing");
        putMultiPinyin("150-229", "yi,xie");
        putMultiPinyin("150-231", "jian,zun");
        putMultiPinyin("150-235", "you,yu");
        putMultiPinyin("150-241", "zhi,yi");
        putMultiPinyin("150-245", "yi,ti");
        putMultiPinyin("150-252", "yu,mou");
        putMultiPinyin("150-253", "za,zan");
        putMultiPinyin("150-254", "kB sei");
    }

    private static void initMap151() {
        putMultiPinyin("151-70", "chen,zhen");
        putMultiPinyin("151-72", "ting,ying");
        putMultiPinyin("151-76", "ben,fan");
        putMultiPinyin("151-86", "su,yin");
        putMultiPinyin("151-93", "xuan,juan,xie");
        putMultiPinyin("151-94", "tu,cha");
        putMultiPinyin("151-96", "ao,you");
        putMultiPinyin("151-101", "ren,er");
        putMultiPinyin("151-116", "si,qi");
        putMultiPinyin("151-123", "chan,yan");
        putMultiPinyin("151-128", "bin,bing");
        putMultiPinyin("151-131", "chou,tao,dao");
        putMultiPinyin("151-140", "cong,song");
        putMultiPinyin("151-145", "de,zhe");
        putMultiPinyin("151-147", "pai,bei,pei");
        putMultiPinyin("151-148", "bang,pou,bei");
        putMultiPinyin("151-152", "li,lie");
        putMultiPinyin("151-168", "quan,juan");
        putMultiPinyin("151-170", "ren,shen");
        putMultiPinyin("151-173", "fu,su");
        putMultiPinyin("151-175", "zou,sou");
        putMultiPinyin("151-184", "jie,qie");
        putMultiPinyin("151-185", "chou,zhou,diao");
        putMultiPinyin("151-188", "cheng,sheng");
        putMultiPinyin("151-189", "zu,cui");
        putMultiPinyin("151-190", "qiang,kong");
        putMultiPinyin("151-209", "quan,juan");
        putMultiPinyin("151-210", "mi eng");
        putMultiPinyin("151-217", "duo,chuan");
        putMultiPinyin("151-219", "wei,hui");
        putMultiPinyin("151-223", "jian,han");
        putMultiPinyin("151-226", "yan,ya");
        putMultiPinyin("151-235", "guo,kua");
        putMultiPinyin("151-249", "ji,zhi");
        putMultiPinyin("151-252", "ku,hu");
    }

    private static void initMap152() {
        putMultiPinyin("152-66", "song,cong");
        putMultiPinyin("152-67", "xuan,yuan");
        putMultiPinyin("152-68", "yang,ying");
        putMultiPinyin("152-71", "die,ye");
        putMultiPinyin("152-74", "shun,dun");
        putMultiPinyin("152-78", "di,shi");
        putMultiPinyin("152-83", "le,yue");
        putMultiPinyin("152-88", "wen,yun");
        putMultiPinyin("152-91", "bi,pi");
        putMultiPinyin("152-94", "zhan,nian,zhen");
        putMultiPinyin("152-95", "fu,bo");
        putMultiPinyin("152-99", "jian,jin");
        putMultiPinyin("152-102", "sha,xie");
        putMultiPinyin("152-141", "qian,lian,xian");
        putMultiPinyin("152-149", "dian,zhen");
        putMultiPinyin("152-155", "xi,die");
        putMultiPinyin("152-156", "ji,gui");
        putMultiPinyin("152-159", "rong,yong");
        putMultiPinyin("152-164", "tuan,shuan,quan");
        putMultiPinyin("152-167", "cui,zhi");
        putMultiPinyin("152-169", "you,chao");
        putMultiPinyin("152-180", "man,wan");
        putMultiPinyin("152-183", "le,yue");
        putMultiPinyin("152-186", "cong,zong");
        putMultiPinyin("152-187", "li,chi");
        putMultiPinyin("152-200", "chao,jiao");
        putMultiPinyin("152-205", "jiu,liao");
        putMultiPinyin("152-210", "niao,mu");
        putMultiPinyin("152-215", "sha,xie");
        putMultiPinyin("152-236", "fa,fei");
        putMultiPinyin("152-239", "rao,nao");
        putMultiPinyin("152-246", "zhan,jian");
        putMultiPinyin("152-250", "tui,dun");
        putMultiPinyin("152-252", "tang,cheng");
        putMultiPinyin("152-254", "su,qiu");
    }

    private static void initMap153() {
        putMultiPinyin("153-65", "tan,dian");
        putMultiPinyin("153-72", "tong,chuang");
        putMultiPinyin("153-73", "zeng,ceng");
        putMultiPinyin("153-74", "fen,fei");
        putMultiPinyin("153-76", "ran,yan");
        putMultiPinyin("153-91", "cu,chu");
        putMultiPinyin("153-93", "shu,qiao");
        putMultiPinyin("153-113", "ping,bo");
        putMultiPinyin("153-117", "gui,hui");
        putMultiPinyin("153-121", "zhai,shi,tu");
        putMultiPinyin("153-132", "chou,tao,dao");
        putMultiPinyin("153-137", "bin,bing");
        putMultiPinyin("153-140", "qian,lian");
        putMultiPinyin("153-141", "ni,mi");
        putMultiPinyin("153-145", "jian,kan");
        putMultiPinyin("153-147", "nou,ruan,ru");
        putMultiPinyin("153-164", "huang,guo,gu");
        putMultiPinyin("153-172", "lv,chu");
        putMultiPinyin("153-173", "mie,mei");
        putMultiPinyin("153-181", "li,yue");
        putMultiPinyin("153-183", "zhuo,zhu");
        putMultiPinyin("153-195", "jue,ji");
        putMultiPinyin("153-198", "huai,gui");
        putMultiPinyin("153-202", "la,lai");
        putMultiPinyin("153-217", "chan,zhan");
        putMultiPinyin("153-222", "wei,zui");
        putMultiPinyin("153-246", "yu,yi");
        putMultiPinyin("153-247", "qian,xian");
        putMultiPinyin("153-250", "chu,qu,xi");
        putMultiPinyin("153-252", "ke,ai");
        putMultiPinyin("153-253", "yi,yin");
    }

    private static void initMap154() {
        putMultiPinyin("154-64", "xi,kai");
        putMultiPinyin("154-70", "shuo,sou");
        putMultiPinyin("154-71", "ei,ai");
        putMultiPinyin("154-72", "xu,chua");
        putMultiPinyin("154-73", "chi,chuai");
        putMultiPinyin("154-75", "kan,qian");
        putMultiPinyin("154-77", "kan,ke");
        putMultiPinyin("154-80", "yan,yin");
        putMultiPinyin("154-86", "jin,qun");
        putMultiPinyin("154-97", "lian,han");
        putMultiPinyin("154-108", "zhi,chi");
        putMultiPinyin("154-111", "se,sha");
        putMultiPinyin("154-122", "mo,wen");
        putMultiPinyin("154-132", "qing,jing");
        putMultiPinyin("154-134", "fou,bo");
        putMultiPinyin("154-135", "ye,yan");
        putMultiPinyin("154-137", "hun,mei");
        putMultiPinyin("154-149", "kui,hui");
        putMultiPinyin("154-160", "qing,keng,sheng");
        putMultiPinyin("154-163", "ke,qiao");
        putMultiPinyin("154-164", "ke,qiao");
        putMultiPinyin("154-165", "xiao,yao");
        putMultiPinyin("154-175", "guan,wan");
        putMultiPinyin("154-195", "dou,nuo");
        putMultiPinyin("154-203", "sai,sui");
        putMultiPinyin("154-222", "yang,ri");
        putMultiPinyin("154-233", "zheng,cheng");
        putMultiPinyin("154-240", "gui,jiu");
        putMultiPinyin("154-241", "bin,pa");
        putMultiPinyin("154-245", "zhuo,que");
    }

    private static void initMap155() {
        putMultiPinyin("155-68", "zhi,ji");
        putMultiPinyin("155-78", "gan,han,cen");
        putMultiPinyin("155-80", "fang,pang");
        putMultiPinyin("155-82", "hu,huang");
        putMultiPinyin("155-83", "niu,you");
        putMultiPinyin("155-92", "nv,niu");
        putMultiPinyin("155-93", "mei,mo");
        putMultiPinyin("155-94", "mi,wu");
        putMultiPinyin("155-96", "hong,pang");
        putMultiPinyin("155-100", "zhui,zi");
        putMultiPinyin("155-107", "tuo,duo");
        putMultiPinyin("155-109", "mi,li");
        putMultiPinyin("155-110", "yi,chi");
        putMultiPinyin("155-117", "yi,die");
        putMultiPinyin("155-123", "chu,she");
        putMultiPinyin("155-124", "you,ao");
        putMultiPinyin("155-128", "peng,ping");
        putMultiPinyin("155-135", "yue,sa");
        putMultiPinyin("155-137", "jue,xue");
        putMultiPinyin("155-155", "se,qi,zi");
        putMultiPinyin("155-161", "an,yan,e");
        putMultiPinyin("155-171", "su,shuo");
        putMultiPinyin("155-173", "qie,jie");
        putMultiPinyin("155-193", "you,di");
        putMultiPinyin("155-198", "ying,cheng");
        putMultiPinyin("155-205", "feng,hong");
        putMultiPinyin("155-212", "sui,nei");
        putMultiPinyin("155-226", "tun,yun");
        putMultiPinyin("155-236", "shou,tao");
        putMultiPinyin("155-239", "kong,nang");
        putMultiPinyin("155-240", "wan,wo,yuan");
        putMultiPinyin("155-249", "qie,ji");
        putMultiPinyin("155-253", "guo,guan");
    }

    private static void initMap156() {
        putMultiPinyin("156-75", "ping,peng");
        putMultiPinyin("156-77", "yu,xu");
        putMultiPinyin("156-81", "jing,cheng");
        putMultiPinyin("156-86", "nian,shen");
        putMultiPinyin("156-87", "biao,hu");
        putMultiPinyin("156-98", "wen,min");
        putMultiPinyin("156-99", "ruo,re,luo");
        putMultiPinyin("156-114", "qiu,wu");
        putMultiPinyin("156-117", "wo,guo");
        putMultiPinyin("156-118", "ti,di");
        putMultiPinyin("156-130", "hong,qing");
        putMultiPinyin("156-147", "hui,min,xu");
        putMultiPinyin("156-161", "min,hun");
        putMultiPinyin("156-168", "tuan,nuan");
        putMultiPinyin("156-169", "qiu,jiao");
        putMultiPinyin("156-171", "tang,shang");
        putMultiPinyin("156-176", "ban,pan");
        putMultiPinyin("156-179", "zhuang,hun");
        putMultiPinyin("156-189", "feng,hong");
        putMultiPinyin("156-196", "yan,gui");
        putMultiPinyin("156-199", "lian,nian,xian");
        putMultiPinyin("156-205", "da,ta");
        putMultiPinyin("156-228", "chu,xu");
        putMultiPinyin("156-233", "hao,xue");
        putMultiPinyin("156-235", "qi,xi,xie");
        putMultiPinyin("156-238", "xing,ying");
        putMultiPinyin("156-245", "ze,hao");
    }

    private static void initMap157() {
        putMultiPinyin("157-71", "hu,xu");
        putMultiPinyin("157-89", "cong,song");
        putMultiPinyin("157-96", "tuan,zhuan");
        putMultiPinyin("157-109", "feng,peng");
        putMultiPinyin("157-112", "ben,peng");
        putMultiPinyin("157-114", "chong,zhuang");
        putMultiPinyin("157-116", "huo,kuo");
        putMultiPinyin("157-120", "liao,liu");
        putMultiPinyin("157-124", "cong,zong");
        putMultiPinyin("157-131", "cong,zong");
        putMultiPinyin("157-135", "pi,pie");
        putMultiPinyin("157-137", "jiao,qiao");
        putMultiPinyin("157-139", "dang,xiang");
        putMultiPinyin("157-148", "xi,ya");
        putMultiPinyin("157-156", "cong,zong");
        putMultiPinyin("157-160", "tan,shan");
        putMultiPinyin("157-162", "kui,hui");
        putMultiPinyin("157-164", "tu,zha");
        putMultiPinyin("157-165", "san,sa");
        putMultiPinyin("157-185", "hong,gong");
        putMultiPinyin("157-198", "mian,sheng");
        putMultiPinyin("157-201", "ze,shi");
        putMultiPinyin("157-207", "wan,man");
        putMultiPinyin("157-210", "kuai,hui");
        putMultiPinyin("157-227", "guo,wo");
        putMultiPinyin("157-229", "fen,pen");
        putMultiPinyin("157-231", "ji,sha");
        putMultiPinyin("157-232", "hui,huo");
        putMultiPinyin("157-236", "ding,ting");
        putMultiPinyin("157-240", "mi,ni");
        putMultiPinyin("157-251", "cui,zui");
    }

    private static void initMap158() {
        putMultiPinyin("158-67", "huo,hu");
        putMultiPinyin("158-70", "jun,xun");
        putMultiPinyin("158-71", "ai,kai,ke");
        putMultiPinyin("158-83", "wei,dui");
        putMultiPinyin("158-84", "luo,po");
        putMultiPinyin("158-85", "zan,cuan");
        putMultiPinyin("158-94", "du,dou");
        putMultiPinyin("158-102", "mie,mo");
        putMultiPinyin("158-106", "cheng,deng");
        putMultiPinyin("158-119", "wei,dui");
        putMultiPinyin("158-120", "huai,wai");
        putMultiPinyin("158-123", "long,shuang");
        putMultiPinyin("158-136", "jian,zun");
        putMultiPinyin("158-143", "rang,nang");
        putMultiPinyin("158-149", "zhuo,jiao,ze");
        putMultiPinyin("158-163", "zan,cuan");
        putMultiPinyin("158-170", "dang,tang");
        putMultiPinyin("158-181", "xun,quan");
        putMultiPinyin("158-193", "zha,yu");
        putMultiPinyin("158-199", "fen,ben");
        putMultiPinyin("158-208", "pang,feng");
        putMultiPinyin("158-227", "zhuo,chu");
        putMultiPinyin("158-228", "pao,fou");
        putMultiPinyin("158-232", "shan,qian");
        putMultiPinyin("158-236", "jiao,yao");
        putMultiPinyin("158-250", "tong,dong");
    }

    private static void initMap159() {
        putMultiPinyin("159-74", "fu,pao");
        putMultiPinyin("159-76", "xie,che");
        putMultiPinyin("159-91", "xun,hun");
        putMultiPinyin("159-93", "juan,ye");
        putMultiPinyin("159-97", "qu,jun");
        putMultiPinyin("159-99", "xie,che");
        putMultiPinyin("159-100", "ji,qi");
        putMultiPinyin("159-113", "chao,ju");
        putMultiPinyin("159-115", "wo,ai");
        putMultiPinyin("159-116", "zong,cong");
        putMultiPinyin("159-121", "xi,yi");
        putMultiPinyin("159-130", "xiong,ying");
        putMultiPinyin("159-135", "xiong,ying");
        putMultiPinyin("159-144", "hui,yun,xun");
        putMultiPinyin("159-154", "shan,qian");
        putMultiPinyin("159-155", "xi,yi");
        putMultiPinyin("159-164", "ye,zha");
        putMultiPinyin("159-184", "en,yun");
        putMultiPinyin("159-192", "he,xiao");
        putMultiPinyin("159-208", "cong,zong");
        putMultiPinyin("159-209", "lu,ao");
        putMultiPinyin("159-212", "peng,feng");
        putMultiPinyin("159-213", "sui,cui");
        putMultiPinyin("159-223", "han,ran");
        putMultiPinyin("159-237", "chan,dan");
        putMultiPinyin("159-247", "jiao,qiao,jue,zhuo");
        putMultiPinyin("159-252", "tong,dong");
    }

    private static void initMap160() {
        putMultiPinyin("160-77", "tai,lie");
        putMultiPinyin("160-104", "rong,ying");
        putMultiPinyin("160-105", "li,lie");
        putMultiPinyin("160-109", "la,lie");
        putMultiPinyin("160-112", "kuang,huang");
        putMultiPinyin("160-119", "yan,xun");
        putMultiPinyin("160-141", "zhao,zhua");
        putMultiPinyin("160-144", "cheng,chen");
        putMultiPinyin("160-164", "bian,mian");
        putMultiPinyin("160-168", "you,yong");
        putMultiPinyin("160-172", "jiu,le");
        putMultiPinyin("160-179", "ge,qiu");
        putMultiPinyin("160-182", "you,chou");
        putMultiPinyin("160-195", "zhi,te");
        putMultiPinyin("160-211", "mao,li");
        putMultiPinyin("160-227", "quan,ba");
        putMultiPinyin("160-230", "zhuo,bao");
        putMultiPinyin("160-232", "kang,gang");
        putMultiPinyin("160-233", "pei,fei");
        putMultiPinyin("160-237", "huan,fan");
        putMultiPinyin("160-244", "yi,quan,chi");
        putMultiPinyin("160-245", "sheng,xing");
        putMultiPinyin("160-246", "tuo,yi");
    }

    private static void initMap170() {
        putMultiPinyin("170-72", "ta,shi");
        putMultiPinyin("170-73", "tong,dong");
        putMultiPinyin("170-75", "mang,dou");
        putMultiPinyin("170-76", "xi,shi");
        putMultiPinyin("170-87", "bai,pi");
        putMultiPinyin("170-92", "jian,yan");
        putMultiPinyin("170-99", "ya,wei");
        putMultiPinyin("170-109", "ya,jia,qie");
        putMultiPinyin("170-110", "xie,he,ge,hai");
        putMultiPinyin("170-112", "bian,pian");
        putMultiPinyin("170-116", "bo,po");
        putMultiPinyin("170-130", "hao,gao");
        putMultiPinyin("170-146", "yao,xiao");
        putMultiPinyin("170-147", "shuo,xi");
        putMultiPinyin("170-152", "ge,lie,xie");
        putMultiPinyin("170-160", "bian,pian");
    }

    private static void initMap171() {
        putMultiPinyin("171-65", "nou,ru");
        putMultiPinyin("171-68", "nao,you");
        putMultiPinyin("171-76", "nao,you");
        putMultiPinyin("171-96", "chang,yang");
        putMultiPinyin("171-106", "men,yun");
        putMultiPinyin("171-108", "jian,qian");
        putMultiPinyin("171-111", "qiang,cang");
        putMultiPinyin("171-113", "an,gan");
        putMultiPinyin("171-116", "xuan,xian");
        putMultiPinyin("171-125", "yi,tai");
        putMultiPinyin("171-126", "zu,ju");
        putMultiPinyin("171-144", "yin,ken");
        putMultiPinyin("171-159", "di,ti");
    }

    private static void initMap172() {
        putMultiPinyin("172-73", "xuan,qiong");
        putMultiPinyin("172-86", "pin,bing");
        putMultiPinyin("172-88", "cui,se");
        putMultiPinyin("172-94", "wei,yu");
        putMultiPinyin("172-97", "beng,pei");
        putMultiPinyin("172-113", "hun,hui");
        putMultiPinyin("172-128", "xie,jie");
        putMultiPinyin("172-132", "chang,yang");
        putMultiPinyin("172-153", "tian,zhen");
        putMultiPinyin("172-154", "qiang,cang");
        putMultiPinyin("172-158", "bin,pian");
        putMultiPinyin("172-159", "tu,shu");
    }

    private static void initMap173() {
        putMultiPinyin("173-70", "zao,suo");
        putMultiPinyin("173-87", "qiong,jue");
        putMultiPinyin("173-103", "hui,kuai");
        putMultiPinyin("173-111", "lu,fu");
        putMultiPinyin("173-112", "bin,pian");
        putMultiPinyin("173-117", "ji,zi");
        putMultiPinyin("173-140", "mi,xi");
        putMultiPinyin("173-142", "qiong,wei");
        putMultiPinyin("173-146", "huan,ye,ya");
        putMultiPinyin("173-148", "bo,pao");
        putMultiPinyin("173-149", "zhi,hu");
        putMultiPinyin("173-152", "xiang,hong");
        putMultiPinyin("173-160", "ki ro ton,mao wa");
    }

    private static void initMap174() {
        putMultiPinyin("174-107", "ting,ding");
        putMultiPinyin("174-110", "bi,qi");
        putMultiPinyin("174-119", "fu,bi");
        putMultiPinyin("174-125", "da,fu");
        putMultiPinyin("174-130", "ce,ji");
        putMultiPinyin("174-131", "zai,zi");
        putMultiPinyin("174-135", "zhi,chou,shi");
        putMultiPinyin("174-137", "fan,pan");
        putMultiPinyin("174-140", "she,yu");
    }

    private static void initMap175() {
        putMultiPinyin("175-67", "jie,qie");
        putMultiPinyin("175-70", "zhi,di");
        putMultiPinyin("175-78", "jue,xue");
        putMultiPinyin("175-80", "ya,xia");
        putMultiPinyin("175-86", "fa,bian");
        putMultiPinyin("175-90", "shan,dian");
        putMultiPinyin("175-92", "teng,chong");
        putMultiPinyin("175-95", "wei,you,yu");
        putMultiPinyin("175-97", "tan,shi");
        putMultiPinyin("175-110", "beng,peng");
        putMultiPinyin("175-114", "ma,lin");
        putMultiPinyin("175-116", "tian,dian");
        putMultiPinyin("175-117", "an,ye,e");
        putMultiPinyin("175-122", "ke,e");
        putMultiPinyin("175-128", "zhi,chi");
        putMultiPinyin("175-144", "hui,lei");
        putMultiPinyin("175-145", "n<e,yao");
        putMultiPinyin("175-146", "dian,chen");
        putMultiPinyin("175-160", "qiao,jiao");
    }

    private static void initMap176() {
        putMultiPinyin("176-73", "gui,wei");
        putMultiPinyin("176-79", "li,lai");
        putMultiPinyin("176-110", "ji,bi");
        putMultiPinyin("176-113", "pa,ba");
        putMultiPinyin("176-119", "gao,yao");
        putMultiPinyin("176-141", "li,luo,bo");
        putMultiPinyin("176-154", "zha,cu");
        putMultiPinyin("176-156", "zhao,zhan,dan");
        putMultiPinyin("176-162", "a,e");
        putMultiPinyin("176-188", "ao,wa");
        putMultiPinyin("176-194", "ao,yu");
        putMultiPinyin("176-199", "ba,pa");
        putMultiPinyin("176-210", "ba,pa");
        putMultiPinyin("176-213", "ba,pi");
        putMultiPinyin("176-216", "bai,bo");
        putMultiPinyin("176-232", "ban,pan");
        putMultiPinyin("176-245", "bang,pang");
        putMultiPinyin("176-246", "bang,beng");
        putMultiPinyin("176-254", "bao,bo");
    }

    private static void initMap177() {
        putMultiPinyin("177-84", "zhou,chou");
        putMultiPinyin("177-90", "mang,wang");
        putMultiPinyin("177-93", "xian,tian");
        putMultiPinyin("177-95", "xi,pan");
        putMultiPinyin("177-100", "yun,hun");
        putMultiPinyin("177-106", "yang,ying");
        putMultiPinyin("177-108", "yao,ao");
        putMultiPinyin("177-114", "ju,xu,kou");
        putMultiPinyin("177-117", "mo,mie");
        putMultiPinyin("177-121", "die,ti");
        putMultiPinyin("177-125", "bing,fang");
        putMultiPinyin("177-126", "pang,pan");
        putMultiPinyin("177-130", "die,zhi");
        putMultiPinyin("177-134", "xuan,shun,xun");
        putMultiPinyin("177-141", "qiao,shao,xiao");
        putMultiPinyin("177-145", "cuo,zhuai");
        putMultiPinyin("177-161", "bao,bo");
        putMultiPinyin("177-164", "bao,bu,pu");
        putMultiPinyin("177-169", "bao,pu");
        putMultiPinyin("177-187", "bei,pi");
        putMultiPinyin("177-217", "bi,pi");
        putMultiPinyin("177-219", "bi,bei");
        putMultiPinyin("177-226", "bian,pian");
        putMultiPinyin("177-227", "bian,pian");
    }

    private static void initMap178() {
        putMultiPinyin("178-66", "sui,zui");
        putMultiPinyin("178-71", "yi,ze,gao");
        putMultiPinyin("178-90", "gui,wei,kui");
        putMultiPinyin("178-93", "kou,ji");
        putMultiPinyin("178-96", "qiong,huan");
        putMultiPinyin("178-102", "diao,dou");
        putMultiPinyin("178-107", "lou,lv");
        putMultiPinyin("178-109", "man,men");
        putMultiPinyin("178-112", "run,shun");
        putMultiPinyin("178-118", "xian,jian");
        putMultiPinyin("178-121", "wu,mi");
        putMultiPinyin("178-122", "gui,kui");
        putMultiPinyin("178-133", "ning,cheng");
        putMultiPinyin("178-136", "huo,yue");
        putMultiPinyin("178-142", "kuang,guo");
        putMultiPinyin("178-149", "guan,quan");
        putMultiPinyin("178-155", "jin,qin,guan");
        putMultiPinyin("178-156", "yu,xu,jue");
        putMultiPinyin("178-170", "bo,bei");
        putMultiPinyin("178-174", "bo,bai,ba");
        putMultiPinyin("178-180", "bo,po");
        putMultiPinyin("178-183", "bu,bo");
        putMultiPinyin("178-187", "bu,fou");
        putMultiPinyin("178-190", "bu,bo");
        putMultiPinyin("178-206", "can,shen,cen,san");
        putMultiPinyin("178-216", "cang,zang");
        putMultiPinyin("178-224", "ce,ze,zhai");
        putMultiPinyin("178-233", "cha,zha");
        putMultiPinyin("178-238", "cha,chai,ci");
        putMultiPinyin("178-240", "chai,ca");
        putMultiPinyin("178-244", "chan,xian,can,shan");
        putMultiPinyin("178-252", "chan,zhan");
    }

    private static void initMap179() {
        putMultiPinyin("179-77", "gang,qiang,kong");
        putMultiPinyin("179-87", "pin,bin,fen");
        putMultiPinyin("179-96", "ke,luo");
        putMultiPinyin("179-113", "kuang,guang");
        putMultiPinyin("179-117", "wei,gui");
        putMultiPinyin("179-119", "ken,xian,gun,yin");
        putMultiPinyin("179-121", "peng,ping");
        putMultiPinyin("179-125", "wei,ai,gai");
        putMultiPinyin("179-130", "que,ke,ku");
        putMultiPinyin("179-137", "mang,bang");
        putMultiPinyin("179-138", "luo,long");
        putMultiPinyin("179-139", "yong,tong");
        putMultiPinyin("179-155", "zhui,chui,duo");
        putMultiPinyin("179-159", "zong,cong");
        putMultiPinyin("179-164", "chang,zhang");
        putMultiPinyin("179-167", "chang,an,han");
        putMultiPinyin("179-175", "zhao,chao");
        putMultiPinyin("179-176", "chao,zhao");
        putMultiPinyin("179-181", "che,ju");
        putMultiPinyin("179-198", "cheng,chen");
        putMultiPinyin("179-203", "cheng,sheng");
        putMultiPinyin("179-206", "cheng,deng");
        putMultiPinyin("179-215", "chi,shi");
        putMultiPinyin("179-223", "chi,che");
        putMultiPinyin("179-240", "chou,qiu");
        putMultiPinyin("179-244", "chou,xiu");
    }

    private static void initMap180() {
        putMultiPinyin("180-68", "jian,zhan");
        putMultiPinyin("180-70", "que,xi");
        putMultiPinyin("180-76", "nao,gang");
        putMultiPinyin("180-84", "shuo,shi");
        putMultiPinyin("180-89", "ti,di");
        putMultiPinyin("180-96", "que,qiao");
        putMultiPinyin("180-99", "su,xie");
        putMultiPinyin("180-102", "si,ti");
        putMultiPinyin("180-104", "hua,ke,gu");
        putMultiPinyin("180-106", "kui,wei");
        putMultiPinyin("180-108", "xia,qia,ya");
        putMultiPinyin("180-110", "lian,qian");
        putMultiPinyin("180-111", "wei,ai,gai");
        putMultiPinyin("180-120", "ao,qiao");
        putMultiPinyin("180-132", "qi,zhu");
        putMultiPinyin("180-139", "lao,luo");
        putMultiPinyin("180-145", "pan,bo");
        putMultiPinyin("180-146", "ji,she");
        putMultiPinyin("180-157", "he,qiao");
        putMultiPinyin("180-158", "ke,huo");
        putMultiPinyin("180-167", "chuai,tuan,zhui");
        putMultiPinyin("180-171", "chuan,zhuan");
        putMultiPinyin("180-177", "chuang,zhuang");
        putMultiPinyin("180-190", "chun,zhun");
        putMultiPinyin("180-194", "chuo,chao");
        putMultiPinyin("180-211", "cong,zong");
        putMultiPinyin("180-233", "cuo,zuo");
        putMultiPinyin("180-241", "da,dB");
        putMultiPinyin("180-243", "da,dai");
    }

    private static void initMap181() {
        putMultiPinyin("181-67", "que,hu");
        putMultiPinyin("181-74", "e,qi");
        putMultiPinyin("181-85", "xian,xin");
        putMultiPinyin("181-111", "zhi,qi");
        putMultiPinyin("181-112", "beng,fang");
        putMultiPinyin("181-122", "mi,bi");
        putMultiPinyin("181-136", "shui,lei");
        putMultiPinyin("181-150", "bi,pi");
        putMultiPinyin("181-153", "you,chao");
        putMultiPinyin("181-165", "dan,shan,chan");
        putMultiPinyin("181-167", "dan,shan");
        putMultiPinyin("181-172", "dan,da");
        putMultiPinyin("181-175", "dan,tan");
        putMultiPinyin("181-195", "de,dei");
        putMultiPinyin("181-196", "di,de");
        putMultiPinyin("181-212", "di,zhai");
        putMultiPinyin("181-215", "di,de");
        putMultiPinyin("181-216", "di,de");
        putMultiPinyin("181-220", "di,ti,tui");
        putMultiPinyin("181-232", "dian,tian");
        putMultiPinyin("181-233", "dian,tian,sheng");
        putMultiPinyin("181-247", "tiao,diao,zhou");
    }

    private static void initMap182() {
        putMultiPinyin("182-64", "shang,yang");
        putMultiPinyin("182-65", "ti,zhi");
        putMultiPinyin("182-85", "shan,chan");
        putMultiPinyin("182-103", "cha,na");
        putMultiPinyin("182-104", "yi,zhi");
        putMultiPinyin("182-109", "hao,mao");
        putMultiPinyin("182-129", "huo,kuo");
        putMultiPinyin("182-131", "shi,zhi");
        putMultiPinyin("182-132", "huo,kuo");
        putMultiPinyin("182-141", "fu,pu");
        putMultiPinyin("182-143", "xun,ze");
        putMultiPinyin("182-149", "tu,shu");
        putMultiPinyin("182-157", "ji,qi");
        putMultiPinyin("182-160", "leng,ling");
        putMultiPinyin("182-161", "ding,zheng");
        putMultiPinyin("182-177", "dong,tong");
        putMultiPinyin("182-188", "du,dou");
        putMultiPinyin("182-190", "du,dai");
        putMultiPinyin("182-193", "du,dou");
        putMultiPinyin("182-200", "du,duo");
        putMultiPinyin("182-210", "dui,rui,yue");
        putMultiPinyin("182-216", "dun,dui");
        putMultiPinyin("182-218", "dun,tun");
        putMultiPinyin("182-233", "duo,hui");
        putMultiPinyin("182-234", "e,yi");
        putMultiPinyin("182-241", "e,wu");
    }

    private static void initMap183() {
        putMultiPinyin("183-66", "zui,zu,su");
        putMultiPinyin("183-71", "xi,qie");
        putMultiPinyin("183-75", "pi,bi");
        putMultiPinyin("183-81", "cheng,chen");
        putMultiPinyin("183-83", "xian,jian,lian");
        putMultiPinyin("183-84", "zi,jiu");
        putMultiPinyin("183-95", "can,shan,cen");
        putMultiPinyin("183-96", "men,mei");
        putMultiPinyin("183-110", "xiao,rao");
        putMultiPinyin("183-113", "zhuo,bo");
        putMultiPinyin("183-114", "tong,zhong");
        putMultiPinyin("183-129", "cheng,chen");
        putMultiPinyin("183-133", "biao,pao");
        putMultiPinyin("183-135", "zhuo,jue");
        putMultiPinyin("183-137", "cuan,zan");
        putMultiPinyin("183-148", "zhu,ku");
        putMultiPinyin("183-149", "jiao,liao,liu");
        putMultiPinyin("183-152", "wa,gui");
        putMultiPinyin("183-172", "fan,pan");
        putMultiPinyin("183-177", "fan,po");
        putMultiPinyin("183-221", "fen,bin");
        putMultiPinyin("183-235", "feng,ping");
        putMultiPinyin("183-240", "fo,fu,bi,bo");
        putMultiPinyin("183-241", "fou,pi");
        putMultiPinyin("183-247", "fu,bi");
    }

    private static void initMap184() {
        putMultiPinyin("184-69", "ya,ye");
        putMultiPinyin("184-75", "tian,dian,yan");
        putMultiPinyin("184-83", "chao,ke");
        putMultiPinyin("184-84", "kuan,cuan");
        putMultiPinyin("184-85", "kuan,cuan");
        putMultiPinyin("184-101", "chu,qi");
        putMultiPinyin("184-108", "qu,kou");
        putMultiPinyin("184-116", "jing,zhen");
        putMultiPinyin("184-125", "ceng,zeng");
        putMultiPinyin("184-133", "le,jin");
        putMultiPinyin("184-143", "zhui,rui");
        putMultiPinyin("184-146", "cen,jin,han");
        putMultiPinyin("184-147", "pi,bi");
        putMultiPinyin("184-151", "da,xia,na");
        putMultiPinyin("184-177", "fu,pi");
        putMultiPinyin("184-199", "gai,ge,he");
        putMultiPinyin("184-219", "gang,jiang");
        putMultiPinyin("184-222", "gao,yao");
        putMultiPinyin("184-237", "ge,yi");
        putMultiPinyin("184-239", "ge,ji");
        putMultiPinyin("184-242", "ge,ha");
        putMultiPinyin("184-248", "gei,ji");
    }

    private static void initMap185() {
        putMultiPinyin("185-65", "fu,fei");
        putMultiPinyin("185-114", "pou,bu,fu,pu");
        putMultiPinyin("185-117", "pai,bei");
        putMultiPinyin("185-120", "tai,chi");
        putMultiPinyin("185-121", "guai,dai");
        putMultiPinyin("185-124", "zhao,dao");
        putMultiPinyin("185-132", "jun,qun");
        putMultiPinyin("185-149", "shi,yi");
        putMultiPinyin("185-150", "yue,yao,chuo");
        putMultiPinyin("185-155", "shuo,xiao,qiao");
        putMultiPinyin("185-227", "guang,an");
        putMultiPinyin("185-234", "gui,jun,qiu");
        putMultiPinyin("185-241", "gui,ju");
        putMultiPinyin("185-247", "gun,hun");
    }

    private static void initMap186() {
        putMultiPinyin("186-84", "gong,gan,long");
        putMultiPinyin("186-85", "peng,pang");
        putMultiPinyin("186-87", "zhuo,huo");
        putMultiPinyin("186-91", "qiang,cang");
        putMultiPinyin("186-97", "zhu,di");
        putMultiPinyin("186-100", "cen,zan,can");
        putMultiPinyin("186-101", "zhuan,zuan,suan");
        putMultiPinyin("186-103", "piao,biao");
        putMultiPinyin("186-105", "tuan,zhuan");
        putMultiPinyin("186-108", "guo,gui");
        putMultiPinyin("186-117", "ce,ji");
        putMultiPinyin("186-128", "mi,mie");
        putMultiPinyin("186-129", "shai,si");
        putMultiPinyin("186-139", "sun,zhuan");
        putMultiPinyin("186-187", "hang,ben");
        putMultiPinyin("186-199", "he,a,ke");
        putMultiPinyin("186-200", "he,ye");
        putMultiPinyin("186-203", "he,hu");
        putMultiPinyin("186-205", "he,huo,hu");
        putMultiPinyin("186-207", "he,ge");
        putMultiPinyin("186-209", "he,hao,mo");
        putMultiPinyin("186-217", "hei,mo");
        putMultiPinyin("186-223", "heng,hng");
        putMultiPinyin("186-224", "heng,peng");
        putMultiPinyin("186-236", "hong,gong");
    }

    private static void initMap187() {
        putMultiPinyin("187-69", "zhen,jian");
        putMultiPinyin("187-79", "fan,pan,bian");
        putMultiPinyin("187-80", "sou,shu");
        putMultiPinyin("187-93", "shen shi,sen si,qie");
        putMultiPinyin("187-125", "sha,chao");
        putMultiPinyin("187-126", "kang,jing");
        putMultiPinyin("187-144", "ce,se");
        putMultiPinyin("187-163", "hu,xia");
        putMultiPinyin("187-181", "huai,pei,pi");
        putMultiPinyin("187-185", "huan,hai");
        putMultiPinyin("187-225", "hui,kuai");
        putMultiPinyin("187-252", "ji,qi");
    }

    private static void initMap188() {
        putMultiPinyin("188-77", "gu,gou");
        putMultiPinyin("188-82", "san,shen");
        putMultiPinyin("188-86", "san,shen");
        putMultiPinyin("188-115", "yue,yao");
        putMultiPinyin("188-116", "hong,gong");
        putMultiPinyin("188-118", "he,ge");
        putMultiPinyin("188-129", "ji,jie");
        putMultiPinyin("188-146", "zha,za");
        putMultiPinyin("188-153", "zha,za");
        putMultiPinyin("188-158", "bo,bi");
        putMultiPinyin("188-169", "ji,qi");
        putMultiPinyin("188-191", "ji,qi");
        putMultiPinyin("188-192", "ji,zhai");
        putMultiPinyin("188-208", "jia,ga,xia");
        putMultiPinyin("188-210", "jia,jiB");
        putMultiPinyin("188-214", "jia,gu");
        putMultiPinyin("188-219", "jia,jie");
        putMultiPinyin("188-247", "jian,kan");
        putMultiPinyin("188-251", "jian,xian");
    }

    private static void initMap189() {
        putMultiPinyin("189-71", "zhen,tian");
        putMultiPinyin("189-92", "gua,kua");
        putMultiPinyin("189-93", "bai,mo");
        putMultiPinyin("189-98", "huan,geng");
        putMultiPinyin("189-101", "xie,jie");
        putMultiPinyin("189-104", "quan,shuan");
        putMultiPinyin("189-105", "gai,ai");
        putMultiPinyin("189-106", "luo,lao");
        putMultiPinyin("189-108", "beng,bing,peng");
        putMultiPinyin("189-111", "gei,ji");
        putMultiPinyin("189-112", "tong,dong");
        putMultiPinyin("189-114", "tiao,diao,dao");
        putMultiPinyin("189-119", "gai,hai");
        putMultiPinyin("189-130", "chi,zhi");
        putMultiPinyin("189-131", "mian,wen,man,wan");
        putMultiPinyin("189-140", "huan,wan");
        putMultiPinyin("189-141", "qin,xian");
        putMultiPinyin("189-158", "yan,xian");
        putMultiPinyin("189-174", "jiang,qiang");
        putMultiPinyin("189-192", "jiao,jue");
        putMultiPinyin("189-196", "jiao,yao");
        putMultiPinyin("189-199", "jiao,jue");
        putMultiPinyin("189-201", "jiao,zhuo");
        putMultiPinyin("189-203", "jiao,chao");
        putMultiPinyin("189-210", "jie,qi");
        putMultiPinyin("189-219", "jie,ju");
        putMultiPinyin("189-226", "jie,xie");
        putMultiPinyin("189-229", "jie,ji");
        putMultiPinyin("189-230", "jie,gai");
        putMultiPinyin("189-367", "gei,ji");
    }

    private static void initMap190() {
        putMultiPinyin("190-67", "zong,zeng");
        putMultiPinyin("190-68", "lin,chen");
        putMultiPinyin("190-70", "li,lie");
        putMultiPinyin("190-78", "ji,qi");
        putMultiPinyin("190-80", "qian,qing,zheng");
        putMultiPinyin("190-93", "lun,guan");
        putMultiPinyin("190-98", "chuo,chao");
        putMultiPinyin("190-103", "tian,tan,chan");
        putMultiPinyin("190-118", "lv,lu");
        putMultiPinyin("190-131", "ji,qi");
        putMultiPinyin("190-133", "chong,zhong");
        putMultiPinyin("190-136", "miao,mao");
        putMultiPinyin("190-138", "xie,ye");
        putMultiPinyin("190-150", "tou,xu,shu");
        putMultiPinyin("190-156", "bian,pian");
        putMultiPinyin("190-157", "gun,yun");
        putMultiPinyin("190-160", "gua,wo");
        putMultiPinyin("190-162", "jin,jing");
        putMultiPinyin("190-176", "jing,ying");
        putMultiPinyin("190-177", "jing,geng");
        putMultiPinyin("190-187", "jing,cheng");
        putMultiPinyin("190-208", "ju,gou");
        putMultiPinyin("190-215", "ju,zui");
        putMultiPinyin("190-228", "ju,gou");
        putMultiPinyin("190-245", "jue,jiao");
    }

    private static void initMap191() {
        putMultiPinyin("191-65", "yun,wen");
        putMultiPinyin("191-87", "ci,cuo,suo");
        putMultiPinyin("191-90", "yun,wen");
        putMultiPinyin("191-92", "cui,sui,shuai");
        putMultiPinyin("191-102", "zai,zeng");
        putMultiPinyin("191-104", "xian,xuan");
        putMultiPinyin("191-115", "suo,su");
        putMultiPinyin("191-116", "yan,yin");
        putMultiPinyin("191-120", "zhuan,juan");
        putMultiPinyin("191-125", "mu,mo");
        putMultiPinyin("191-138", "mou,miu,miao,mu,liao");
        putMultiPinyin("191-155", "ju,jue");
        putMultiPinyin("191-163", "jun,xun");
        putMultiPinyin("191-167", "ka,ga");
        putMultiPinyin("191-168", "qia,ka");
        putMultiPinyin("191-169", "ka,luo,lo,ge");
        putMultiPinyin("191-172", "kai,jie");
        putMultiPinyin("191-184", "kang,gang");
        putMultiPinyin("191-193", "ke,he");
        putMultiPinyin("191-199", "ke,qiao");
        putMultiPinyin("191-200", "ke,hai");
        putMultiPinyin("191-212", "hang,keng");
        putMultiPinyin("191-230", "kua,ku");
        putMultiPinyin("191-254", "kui,gui");
    }

    private static void initMap192() {
        putMultiPinyin("192-77", "xi,ji");
        putMultiPinyin("192-82", "qiao,sao");
        putMultiPinyin("192-85", "jiao,zhuo");
        putMultiPinyin("192-87", "dan,tan,chan");
        putMultiPinyin("192-98", "pu,fu");
        putMultiPinyin("192-102", "yao,li");
        putMultiPinyin("192-118", "xiang,rang");
        putMultiPinyin("192-123", "li,xi,sa");
        putMultiPinyin("192-136", "yun,wen");
        putMultiPinyin("192-138", "cui,sui,shuai");
        putMultiPinyin("192-163", "kui,hui");
        putMultiPinyin("192-168", "kuo,gua");
        putMultiPinyin("192-178", "lB,la");
        putMultiPinyin("192-209", "mu,lao");
        putMultiPinyin("192-211", "lao,luo");
        putMultiPinyin("192-213", "le,lei");
        putMultiPinyin("192-214", "le,yue");
        putMultiPinyin("192-223", "lei,le");
        putMultiPinyin("192-226", "leng,ling");
    }

    private static void initMap193() {
        putMultiPinyin("193-76", "guai,gua");
        putMultiPinyin("193-84", "ba,pi");
        putMultiPinyin("193-102", "yang,xiang");
        putMultiPinyin("193-111", "mei,gao");
        putMultiPinyin("193-112", "yi,xi");
        putMultiPinyin("193-122", "qiang,kong");
        putMultiPinyin("193-123", "qian,xian,yan");
        putMultiPinyin("193-135", "hong,gong");
        putMultiPinyin("193-145", "pi,bi,po");
        putMultiPinyin("193-148", "qu,yu");
        putMultiPinyin("193-159", "dao,zhou");
        putMultiPinyin("193-165", "li,dai");
        putMultiPinyin("193-169", "liang,lia");
        putMultiPinyin("193-202", "lao,liao");
        putMultiPinyin("193-203", "liao,le");
        putMultiPinyin("193-249", "liu,lu");
    }

    private static void initMap194() {
        putMultiPinyin("194-74", "hou,qu");
        putMultiPinyin("194-88", "ruan,nuo");
        putMultiPinyin("194-89", "er,nai");
        putMultiPinyin("194-90", "duan,zhuan");
        putMultiPinyin("194-93", "si,chi");
        putMultiPinyin("194-94", "qu,chu");
        putMultiPinyin("194-99", "ji,jie");
        putMultiPinyin("194-100", "zha,ze");
        putMultiPinyin("194-109", "yun,ying");
        putMultiPinyin("194-118", "wa,tui,zhuo");
        putMultiPinyin("194-120", "er,nv");
        putMultiPinyin("194-122", "tie,zhe");
        putMultiPinyin("194-130", "di,zhi");
        putMultiPinyin("194-158", "ni,jian");
        putMultiPinyin("194-181", "lu,liu");
        putMultiPinyin("194-202", "shuai,lv");
        putMultiPinyin("194-204", "lv,lu");
        putMultiPinyin("194-218", "lun,guan");
        putMultiPinyin("194-228", "luo,la,lao");
        putMultiPinyin("194-231", "luo,lao");
        putMultiPinyin("194-240", "ma,mB");
        putMultiPinyin("194-241", "mai,man");
        putMultiPinyin("194-247", "man,men");
        putMultiPinyin("194-251", "man,wan");
    }

    private static void initMap195() {
        putMultiPinyin("195-74", "bo,di");
        putMultiPinyin("195-81", "qin,han");
        putMultiPinyin("195-84", "pang,pan");
        putMultiPinyin("195-89", "pi,bi");
        putMultiPinyin("195-94", "fei,bi");
        putMultiPinyin("195-99", "zi,fei");
        putMultiPinyin("195-100", "fei,ku");
        putMultiPinyin("195-103", "ping,peng");
        putMultiPinyin("195-105", "fu,zhou");
        putMultiPinyin("195-118", "gui,kui");
        putMultiPinyin("195-165", "meng,mang");
        putMultiPinyin("195-176", "mao,mo");
        putMultiPinyin("195-180", "me,yao,mB");
        putMultiPinyin("195-187", "mei,mo");
        putMultiPinyin("195-211", "mi,mei");
        putMultiPinyin("195-216", "mi,bi");
        putMultiPinyin("195-218", "mi,bi");
    }

    private static void initMap196() {
        putMultiPinyin("196-114", "lu,biao");
        putMultiPinyin("196-120", "zhuan,chuan,chun");
        putMultiPinyin("196-135", "fan,pan");
        putMultiPinyin("196-138", "hu,wu");
        putMultiPinyin("196-151", "la,ge");
        putMultiPinyin("196-159", "wo,yue");
        putMultiPinyin("196-163", "mo,mu");
        putMultiPinyin("196-166", "mo,ma");
        putMultiPinyin("196-168", "mo,ma");
        putMultiPinyin("196-170", "mo,mu");
        putMultiPinyin("196-178", "mou,mu");
        putMultiPinyin("196-196", "na,nei,nB,ne");
        putMultiPinyin("196-197", "na,ne");
        putMultiPinyin("196-199", "na,nei");
        putMultiPinyin("196-200", "na,nuo");
        putMultiPinyin("196-207", "nan,na");
        putMultiPinyin("196-209", "nan,nuo");
        putMultiPinyin("196-215", "nao,chuo,zhuo");
        putMultiPinyin("196-218", "nei,na");
        putMultiPinyin("196-220", "neng,nai");
        putMultiPinyin("196-231", "ni,niao");
        putMultiPinyin("196-237", "nian,nie");
        putMultiPinyin("196-242", "niao,sui");
        putMultiPinyin("196-254", "ning,zhu");
    }

    private static void initMap197() {
        putMultiPinyin("197-77", "ni,luan");
        putMultiPinyin("197-79", "qian,xian");
        putMultiPinyin("197-81", "guang,jiong");
        putMultiPinyin("197-83", "guang,jiong");
        putMultiPinyin("197-88", "mian,bian");
        putMultiPinyin("197-92", "die,zhi");
        putMultiPinyin("197-93", "zhi,jin");
        putMultiPinyin("197-154", "qiang");
        putMultiPinyin("197-170", "nong,long");
        putMultiPinyin("197-177", "n<e,yao");
        putMultiPinyin("197-212", "pang,bang");
        putMultiPinyin("197-214", "pang,pan");
        putMultiPinyin("197-217", "pao,bao");
        putMultiPinyin("197-218", "pao,bao");
        putMultiPinyin("197-237", "peng,bang");
    }

    private static void initMap198() {
        putMultiPinyin("198-79", "chai,cha");
        putMultiPinyin("198-83", "hu,xia");
        putMultiPinyin("198-85", "hui,hu");
        putMultiPinyin("198-88", "tun,chun");
        putMultiPinyin("198-94", "xu,zhu");
        putMultiPinyin("198-95", "lun,hua");
        putMultiPinyin("198-103", "chan,yin");
        putMultiPinyin("198-108", "di,ti");
        putMultiPinyin("198-114", "zhu,ning");
        putMultiPinyin("198-116", "pa,bo");
        putMultiPinyin("198-122", "zuo,zha");
        putMultiPinyin("198-129", "sheng,rui");
        putMultiPinyin("198-181", "pin,bin");
        putMultiPinyin("198-187", "ping,peng");
        putMultiPinyin("198-193", "ping,bing");
        putMultiPinyin("198-200", "po,pai");
        putMultiPinyin("198-210", "pu,bu");
        putMultiPinyin("198-211", "piao,pu,po");
        putMultiPinyin("198-216", "pu,bao");
        putMultiPinyin("198-217", "pu,bao");
        putMultiPinyin("198-218", "qi,ji");
        putMultiPinyin("198-220", "qi,xi");
        putMultiPinyin("198-228", "qi,ji");
        putMultiPinyin("198-230", "qi,ji");
        putMultiPinyin("198-241", "qi,kai");
        putMultiPinyin("198-245", "qi,qie,xie");
        putMultiPinyin("198-246", "qi,qie");
    }

    private static void initMap199() {
        putMultiPinyin("199-108", "peng,feng");
        putMultiPinyin("199-120", "su,you");
        putMultiPinyin("199-122", "shao,xiao");
        putMultiPinyin("199-124", "wen,wan,mian");
        putMultiPinyin("199-136", "zou,chu");
        putMultiPinyin("199-140", "nie,ren");
        putMultiPinyin("199-143", "zi,zai");
        putMultiPinyin("199-157", "jie,sha");
        putMultiPinyin("199-159", "qiao,zhao");
        putMultiPinyin("199-160", "tai,zhi,chi");
        putMultiPinyin("199-166", "qian,yan");
        putMultiPinyin("199-172", "qian,gan");
        putMultiPinyin("199-179", "qian,jian");
        putMultiPinyin("199-181", "qian,zan,jian");
        putMultiPinyin("199-182", "qian,kan");
        putMultiPinyin("199-191", "qiang,jiang");
        putMultiPinyin("199-192", "qiang,cheng");
        putMultiPinyin("199-202", "qiao,shao");
        putMultiPinyin("199-206", "qiao,xiao");
        putMultiPinyin("199-209", "jia,qie");
        putMultiPinyin("199-210", "qie,ju");
        putMultiPinyin("199-215", "qin,qing");
        putMultiPinyin("199-247", "qu,cu");
        putMultiPinyin("199-248", "qu,ou");
        putMultiPinyin("199-254", "qu,ju");
    }

    private static void initMap200() {
        putMultiPinyin("200-66", "qin,jin");
        putMultiPinyin("200-72", "lin,ma");
        putMultiPinyin("200-164", "qu,cu");
        putMultiPinyin("200-166", "quan,juan");
        putMultiPinyin("200-175", "quan,xuan");
        putMultiPinyin("200-184", "que,qiao");
        putMultiPinyin("200-244", "ruo,re");
        putMultiPinyin("200-247", "sa,xi");
        putMultiPinyin("200-250", "sai,xi");
        putMultiPinyin("200-251", "sai,se");
    }

    private static void initMap201() {
        putMultiPinyin("201-83", "ru,na");
        putMultiPinyin("201-86", "yuan,huan");
        putMultiPinyin("201-91", "xu,shu");
        putMultiPinyin("201-119", "gai,ge,he");
        putMultiPinyin("201-124", "yao,zhuo");
        putMultiPinyin("201-137", "diao,tiao,di");
        putMultiPinyin("201-146", "qiu,xu,fu");
        putMultiPinyin("201-155", "zi,ju");
        putMultiPinyin("201-175", "suo,sha");
        putMultiPinyin("201-178", "cha,sha");
        putMultiPinyin("201-188", "shan,sha");
        putMultiPinyin("201-209", "chang,shBng");
        putMultiPinyin("201-210", "shao,sao");
        putMultiPinyin("201-223", "she,yi");
        putMultiPinyin("201-227", "she,nie");
        putMultiPinyin("201-228", "she,ye,yi");
        putMultiPinyin("201-242", "shen,chen");
    }

    private static void initMap202() {
        putMultiPinyin("202-161", "sheng,xing");
        putMultiPinyin("202-162", "sheng,cheng");
        putMultiPinyin("202-175", "shi,dan");
        putMultiPinyin("202-176", "shi,she");
        putMultiPinyin("202-178", "shi,shen");
        putMultiPinyin("202-179", "shi,si,yi");
        putMultiPinyin("202-182", "shi,zhi");
        putMultiPinyin("202-207", "shi,zhi");
        putMultiPinyin("202-244", "shu,zhu");
        putMultiPinyin("202-245", "shu,zhu");
        putMultiPinyin("202-253", "shu,shuo");
    }

    private static void initMap203() {
        putMultiPinyin("203-64", "xi,xiao");
        putMultiPinyin("203-72", "wan,luan");
        putMultiPinyin("203-78", "qiang,se");
        putMultiPinyin("203-87", "xian,lian");
        putMultiPinyin("203-94", "hao,kao");
        putMultiPinyin("203-101", "yuan,wei");
        putMultiPinyin("203-103", "chou,zhou");
        putMultiPinyin("203-104", "mai,wo");
        putMultiPinyin("203-114", "xiao,hao");
        putMultiPinyin("203-121", "diao,zhuo");
        putMultiPinyin("203-142", "yao,yue");
        putMultiPinyin("203-145", "biao,pao");
        putMultiPinyin("203-160", "zhu,chu");
        putMultiPinyin("203-181", "shuo,shui,yue");
        putMultiPinyin("203-182", "shuo,shi");
        putMultiPinyin("203-188", "si,sai");
        putMultiPinyin("203-197", "si,ci");
        putMultiPinyin("203-198", "si,shi");
        putMultiPinyin("203-222", "su,xiu");
        putMultiPinyin("203-229", "sui,duo");
        putMultiPinyin("203-239", "sun,xun");
        putMultiPinyin("203-245", "suo,su");
        putMultiPinyin("203-253", "ta,jie");
    }

    private static void initMap204() {
        putMultiPinyin("204-192", "tang,shang");
        putMultiPinyin("204-200", "tang,chang");
        putMultiPinyin("204-202", "tang,chang");
        putMultiPinyin("204-225", "ti,di");
        putMultiPinyin("204-238", "tian,zhen");
        putMultiPinyin("204-248", "tiao,tao");
    }

    private static void initMap205() {
        putMultiPinyin("205-75", "fang,bang");
        putMultiPinyin("205-86", "qi,zhi");
        putMultiPinyin("205-87", "yuan,wan");
        putMultiPinyin("205-88", "jue,que");
        putMultiPinyin("205-90", "qin,qian");
        putMultiPinyin("205-102", "dai,de");
        putMultiPinyin("205-109", "gou,qu,xu");
        putMultiPinyin("205-111", "pi,bo");
        putMultiPinyin("205-120", "ge,luo");
        putMultiPinyin("205-123", "mang,bang");
        putMultiPinyin("205-130", "yi,xu");
        putMultiPinyin("205-137", "qie,ni");
        putMultiPinyin("205-205", "tun,zhun");
        putMultiPinyin("205-216", "tuo,ta,zhi");
        putMultiPinyin("205-219", "wa,wB");
        putMultiPinyin("205-240", "wan,yuan");
        putMultiPinyin("205-242", "wan,mo");
        putMultiPinyin("205-246", "wang,wu");
    }

    private static void initMap206() {
        putMultiPinyin("206-151", "nai,neng");
        putMultiPinyin("206-152", "he,xia");
        putMultiPinyin("206-154", "gui,hui");
        putMultiPinyin("206-178", "wei,yi");
        putMultiPinyin("206-190", "wei,yu");
        putMultiPinyin("206-206", "zhua,wo");
        putMultiPinyin("206-208", "wo,guo");
        putMultiPinyin("206-211", "wo,guan");
        putMultiPinyin("206-225", "wu,yu");
    }

    private static void initMap207() {
        putMultiPinyin("207-72", "die,zhi");
        putMultiPinyin("207-74", "qu,ju");
        putMultiPinyin("207-77", "chan,jian");
        putMultiPinyin("207-110", "fei,ben");
        putMultiPinyin("207-111", "lao,liao");
        putMultiPinyin("207-114", "yin,xun");
        putMultiPinyin("207-179", "xian,xi");
        putMultiPinyin("207-180", "xi,xian");
        putMultiPinyin("207-181", "xi,ji");
        putMultiPinyin("207-183", "xi,hu");
        putMultiPinyin("207-195", "sha,xia");
        putMultiPinyin("207-197", "xia,he");
        putMultiPinyin("207-203", "xian,qian");
        putMultiPinyin("207-216", "xian,xuan");
        putMultiPinyin("207-234", "xiang,yang");
        putMultiPinyin("207-239", "xiang,hang");
        putMultiPinyin("207-247", "xiao,xue");
    }

    private static void initMap208() {
        putMultiPinyin("208-136", "bao,pao");
        putMultiPinyin("208-141", "ju,jie");
        putMultiPinyin("208-142", "he,ke");
        putMultiPinyin("208-156", "na,jue");
        putMultiPinyin("208-163", "xiao,jiao");
        putMultiPinyin("208-169", "xie,suo");
        putMultiPinyin("208-174", "xie,jia");
        putMultiPinyin("208-176", "xie,ya,ye,yu,xu");
        putMultiPinyin("208-185", "xie,yi");
        putMultiPinyin("208-197", "xin,shen");
        putMultiPinyin("208-208", "xing,hang,heng");
        putMultiPinyin("208-221", "xiu,xu");
        putMultiPinyin("208-234", "xu,shi");
        putMultiPinyin("208-237", "xu,hu");
        putMultiPinyin("208-243", "xu,chu");
    }

    private static void initMap209() {
        putMultiPinyin("209-68", "chi,nuo");
        putMultiPinyin("209-69", "chi,qi,duo,nuo");
        putMultiPinyin("209-73", "jian,zun");
        putMultiPinyin("209-74", "bo,mo");
        putMultiPinyin("209-79", "gui,gua");
        putMultiPinyin("209-92", "ge,jie");
        putMultiPinyin("209-110", "chou,dao");
        putMultiPinyin("209-114", "yuan,gun");
        putMultiPinyin("209-115", "yan,an");
        putMultiPinyin("209-189", "ya,yB");
        putMultiPinyin("209-202", "yan,ye");
        putMultiPinyin("209-242", "yang,xiang");
        putMultiPinyin("209-246", "yang,ang");
    }

    private static void initMap210() {
        putMultiPinyin("210-134", "ba,po");
        putMultiPinyin("210-138", "jian,xian");
        putMultiPinyin("210-143", "jue,jiao");
        putMultiPinyin("210-148", "pie,mie");
        putMultiPinyin("210-153", "jue,jiao");
        putMultiPinyin("210-182", "ye,xie");
        putMultiPinyin("210-201", "yi,ni");
        putMultiPinyin("210-243", "yin,yan");
    }

    private static void initMap211() {
        putMultiPinyin("211-72", "qin,qing");
        putMultiPinyin("211-83", "jian,bian");
        putMultiPinyin("211-84", "luo,luan");
        putMultiPinyin("211-88", "jue,jiao");
        putMultiPinyin("211-105", "hua,xie");
        putMultiPinyin("211-110", "jie,xie");
        putMultiPinyin("211-115", "ji,qi");
        putMultiPinyin("211-123", "xue,hu");
        putMultiPinyin("211-128", "li,lu");
        putMultiPinyin("211-191", "yong,chong");
        putMultiPinyin("211-225", "yu,shu");
        putMultiPinyin("211-228", "yu,tou");
        putMultiPinyin("211-245", "xu,yu");
    }

    private static void initMap212() {
        putMultiPinyin("212-177", "yuan,yun");
        putMultiPinyin("212-188", "yue,yao");
        putMultiPinyin("212-191", "yue,yao");
        putMultiPinyin("212-219", "zan,za,zBn");
        putMultiPinyin("212-220", "zan,cuan");
        putMultiPinyin("212-241", "ze,zhai");
        putMultiPinyin("212-243", "ze,shi");
        putMultiPinyin("212-248", "zeng,ceng");
        putMultiPinyin("212-250", "za,zha");
        putMultiPinyin("212-251", "zha,cha");
    }

    private static void initMap213() {
        putMultiPinyin("213-65", "tiao,diao");
        putMultiPinyin("213-66", "yi,chi");
        putMultiPinyin("213-79", "ei,xi");
        putMultiPinyin("213-82", "bei,bo");
        putMultiPinyin("213-102", "shuo,shui,yue");
        putMultiPinyin("213-104", "shuo,shui,yue");
        putMultiPinyin("213-108", "shui,shei");
        putMultiPinyin("213-111", "qu,jue");
        putMultiPinyin("213-118", "chi,lai");
        putMultiPinyin("213-121", "ni,na");
        putMultiPinyin("213-123", "diao,tiao");
        putMultiPinyin("213-124", "pi,bei");
        putMultiPinyin("213-139", "ze,zuo,zha,cuo");
        putMultiPinyin("213-145", "chu,ji");
        putMultiPinyin("213-146", "xia,hao");
        putMultiPinyin("213-156", "shi,di");
        putMultiPinyin("213-160", "hua,gua");
        putMultiPinyin("213-164", "zha,shan,shi,ce");
        putMultiPinyin("213-166", "zha,za");
        putMultiPinyin("213-179", "zhan,nian");
        putMultiPinyin("213-183", "zhan,nian");
        putMultiPinyin("213-184", "zhan,chan");
        putMultiPinyin("213-217", "zhao,shao");
        putMultiPinyin("213-219", "zhe,she");
        putMultiPinyin("213-226", "zhe,zhei");
    }

    private static void initMap214() {
        putMultiPinyin("214-76", "xi,shai,ai");
        putMultiPinyin("214-166", "zhi,qi");
        putMultiPinyin("214-168", "zhi,zi");
        putMultiPinyin("214-179", "zhi,shi");
        putMultiPinyin("214-197", "zhi,shi");
        putMultiPinyin("214-216", "zhong,chong");
        putMultiPinyin("214-224", "zhou,yu");
        putMultiPinyin("214-236", "zhu,shu");
        putMultiPinyin("214-248", "zhu,zhuo,zhe");
        putMultiPinyin("214-250", "zhu,chu");
    }

    private static void initMap215() {
        putMultiPinyin("215-122", "juan,xuan");
        putMultiPinyin("215-130", "yi,tui");
        putMultiPinyin("215-158", "zhou,chou");
        putMultiPinyin("215-166", "zhao,zhua");
        putMultiPinyin("215-167", "zhuai,ye");
        putMultiPinyin("215-181", "zhui,chui");
        putMultiPinyin("215-183", "zhui,dui");
        putMultiPinyin("215-193", "zhuo,zuo");
        putMultiPinyin("215-197", "zhuo,zhao,zhe");
        putMultiPinyin("215-200", "zi,ci");
        putMultiPinyin("215-208", "zi,zai");
        putMultiPinyin("215-219", "zong,zeng");
        putMultiPinyin("215-228", "zu,cu");
        putMultiPinyin("215-245", "zuo,zha");
    }

    private static void initMap216() {
        putMultiPinyin("216-126", "ken,kun");
        putMultiPinyin("216-128", "he,mo");
        putMultiPinyin("216-139", "ju,lou");
        putMultiPinyin("216-146", "yuan,yun");
        putMultiPinyin("216-159", "ze,zhai");
        putMultiPinyin("216-162", "qi,ji");
        putMultiPinyin("216-174", "yu,ou");
        putMultiPinyin("216-177", "tuo,zhe");
        putMultiPinyin("216-189", "ji,qi");
        putMultiPinyin("216-191", "mie,nie");
        putMultiPinyin("216-209", "kui,gui");
        putMultiPinyin("216-223", "yan,shan");
        putMultiPinyin("216-238", "yi,ge");
        putMultiPinyin("216-247", "cang,chen");
        putMultiPinyin("216-253", "yi,die");
        putMultiPinyin("216-254", "gou,kou");
    }

    private static void initMap217() {
        putMultiPinyin("217-74", "dai,te");
        putMultiPinyin("217-83", "bi,ben");
        putMultiPinyin("217-90", "jia,gu");
        putMultiPinyin("217-130", "xiong,min");
        putMultiPinyin("217-141", "zhuan,zuan");
        putMultiPinyin("217-164", "qie,jia,ga");
        putMultiPinyin("217-166", "er,nai");
        putMultiPinyin("217-185", "si,qi");
        putMultiPinyin("217-193", "wo,wei");
        putMultiPinyin("217-202", "ji,jie");
        putMultiPinyin("217-205", "lv,lou");
        putMultiPinyin("217-215", "tong,zhuang");
    }

    private static void initMap218() {
        putMultiPinyin("218-103", "die,tu");
        putMultiPinyin("218-108", "ji,jie");
        putMultiPinyin("218-111", "gua,huo");
        putMultiPinyin("218-124", "que,qi,ji");
        putMultiPinyin("218-133", "qu,cu");
        putMultiPinyin("218-140", "ti,yue");
        putMultiPinyin("218-143", "kua,wu");
        putMultiPinyin("218-145", "jue,gui");
        putMultiPinyin("218-147", "fang,pang");
        putMultiPinyin("218-149", "ba,pao");
        putMultiPinyin("218-153", "jian,chen");
        putMultiPinyin("218-177", "yi,dai");
        putMultiPinyin("218-181", "jie,ji");
        putMultiPinyin("218-192", "ei,xi");
        putMultiPinyin("218-243", "wei,kui");
    }

    private static void initMap219() {
        putMultiPinyin("219-64", "dian,tie,die");
        putMultiPinyin("219-65", "pan,ban");
        putMultiPinyin("219-66", "ju,qie");
        putMultiPinyin("219-70", "dai,duo,chi");
        putMultiPinyin("219-77", "pian,beng");
        putMultiPinyin("219-83", "shu,chou");
        putMultiPinyin("219-167", "qie,xi");
        putMultiPinyin("219-168", "xun,huan");
        putMultiPinyin("219-170", "li,zhi");
        putMultiPinyin("219-193", "ge,jia");
        putMultiPinyin("219-201", "kan,qian");
        putMultiPinyin("219-204", "si,mou");
        putMultiPinyin("219-215", "wei,xu");
        putMultiPinyin("219-223", "qi,yin");
        putMultiPinyin("219-230", "di,chi");
        putMultiPinyin("219-237", "dong,tong");
        putMultiPinyin("219-239", "yan,shan");
        putMultiPinyin("219-249", "yuan,huan");
    }

    private static void initMap220() {
        putMultiPinyin("220-86", "li,luo");
        putMultiPinyin("220-97", "sa,xie");
        putMultiPinyin("220-135", "che,ju");
        putMultiPinyin("220-136", "ya,zha,ga");
        putMultiPinyin("220-140", "xin,xian");
        putMultiPinyin("220-143", "fan,gui");
        putMultiPinyin("220-161", "peng,beng");
        putMultiPinyin("220-190", "yuan,yan");
        putMultiPinyin("220-192", "fei,fu");
        putMultiPinyin("220-196", "ju,qu");
        putMultiPinyin("220-197", "bi,pi");
        putMultiPinyin("220-204", "wu,hu");
        putMultiPinyin("220-230", "tiao,shao");
    }

    private static void initMap221() {
        putMultiPinyin("221-161", "qian,xun");
        putMultiPinyin("221-178", "xian,lian");
        putMultiPinyin("221-179", "fu,piao");
        putMultiPinyin("221-183", "shen,xin");
        putMultiPinyin("221-184", "guan,wan");
        putMultiPinyin("221-185", "lang,liang");
        putMultiPinyin("221-210", "wan,yun");
        putMultiPinyin("221-216", "shen,ren");
        putMultiPinyin("221-222", "kui,kuai");
    }

    private static void initMap222() {
        putMultiPinyin("222-120", "dao,bian");
        putMultiPinyin("222-130", "wang,kuang");
        putMultiPinyin("222-140", "zhi,li");
        putMultiPinyin("222-142", "zhu,wang");
        putMultiPinyin("222-164", "liao,lu");
        putMultiPinyin("222-202", "zang,zhuang");
        putMultiPinyin("222-213", "pan,pin,fan");
        putMultiPinyin("222-214", "ao,niu");
        putMultiPinyin("222-215", "jie,jia");
        putMultiPinyin("222-217", "za,zan");
        putMultiPinyin("222-219", "luo,lv");
        putMultiPinyin("222-233", "she,die,ye");
    }

    private static void initMap223() {
        putMultiPinyin("223-128", "huan,hai");
        putMultiPinyin("223-134", "li,chi");
        putMultiPinyin("223-146", "kang,hang");
        putMultiPinyin("223-168", "pi,bo");
        putMultiPinyin("223-175", "te,tui");
        putMultiPinyin("223-182", "tao,dao");
        putMultiPinyin("223-188", "fu,?");
        putMultiPinyin("223-193", "pi,bi");
        putMultiPinyin("223-194", "bei,bai");
        putMultiPinyin("223-195", "wai,he,wo,wa,gua,guo");
        putMultiPinyin("223-197", "yin,shen");
        putMultiPinyin("223-201", "gua,gu");
        putMultiPinyin("223-210", "ji,xi,qia");
        putMultiPinyin("223-218", "zi,ci");
        putMultiPinyin("223-220", "yue,hui");
        putMultiPinyin("223-226", "ji,jie,zhai");
        putMultiPinyin("223-231", "gen,hen");
        putMultiPinyin("223-246", "nuo,re");
        putMultiPinyin("223-248", "lan,lin");
        putMultiPinyin("223-250", "zhou,zhao,tiao");
        putMultiPinyin("223-253", "cui,qi");
    }

    private static void initMap224() {
        putMultiPinyin("224-64", "gai,hai");
        putMultiPinyin("224-85", "xiao,ao");
        putMultiPinyin("224-168", "chuo,chuai");
        putMultiPinyin("224-169", "die,zha");
        putMultiPinyin("224-170", "ta,da");
        putMultiPinyin("224-184", "o,wo");
        putMultiPinyin("224-196", "sha,a");
        putMultiPinyin("224-201", "yi,ai");
        putMultiPinyin("224-203", "hai,hei");
        putMultiPinyin("224-229", "jue,xue");
        putMultiPinyin("224-234", "ca,cha");
        putMultiPinyin("224-237", "wei,guo");
        putMultiPinyin("224-238", "jian,nan");
        putMultiPinyin("224-247", "huan,yuan");
        putMultiPinyin("224-251", "tang,nu");
        putMultiPinyin("224-252", "chou,dao");
    }

    private static void initMap225() {
        putMultiPinyin("225-93", "tan,dan");
        putMultiPinyin("225-98", "qiu,chou");
        putMultiPinyin("225-112", "chan,chen");
        putMultiPinyin("225-119", "po,fa");
        putMultiPinyin("225-121", "yi,shi");
        putMultiPinyin("225-122", "yan,lian,xian");
        putMultiPinyin("225-160", "qiao,jiao");
        putMultiPinyin("225-188", "tong,dong");
        putMultiPinyin("225-189", "jiao,qiao");
        putMultiPinyin("225-203", "wai,wei");
        putMultiPinyin("225-221", "pang,fang");
        putMultiPinyin("225-231", "zhi,zheng");
        putMultiPinyin("225-234", "shan,xian");
        putMultiPinyin("225-237", "han,an");
    }

    private static void initMap226() {
        putMultiPinyin("226-68", "hua,yu");
        putMultiPinyin("226-69", "hua,wu");
        putMultiPinyin("226-74", "ri,ren,jian");
        putMultiPinyin("226-75", "di,dai");
        putMultiPinyin("226-80", "shi,yi");
        putMultiPinyin("226-86", "ri,ren,jian");
        putMultiPinyin("226-87", "pi,zhao");
        putMultiPinyin("226-88", "ye,ya");
        putMultiPinyin("226-186", "zhi,zhong");
        putMultiPinyin("226-219", "jin,qin");
        putMultiPinyin("226-236", "song,zhong");
        putMultiPinyin("226-244", "zuo,zha");
    }

    private static void initMap227() {
        putMultiPinyin("227-125", "xiang,jiong");
        putMultiPinyin("227-131", "yu,si");
        putMultiPinyin("227-132", "xu,hui");
        putMultiPinyin("227-136", "shan,shuo");
        putMultiPinyin("227-137", "chi,li");
        putMultiPinyin("227-138", "xian,xi");
        putMultiPinyin("227-144", "hou,xiang");
        putMultiPinyin("227-147", "diao,tiao,yao");
        putMultiPinyin("227-148", "xian,kuo,tian,gua");
        putMultiPinyin("227-166", "kui,li");
        putMultiPinyin("227-187", "qian,qie");
        putMultiPinyin("227-196", "hui,duo");
        putMultiPinyin("227-219", "kan,han");
        putMultiPinyin("227-233", "gu,yu");
        putMultiPinyin("227-235", "wen,men");
        putMultiPinyin("227-241", "long,shuang");
        putMultiPinyin("227-245", "tuo,duo");
        putMultiPinyin("227-248", "luo,po");
    }

    private static void initMap228() {
        putMultiPinyin("228-75", "shi,zhi");
        putMultiPinyin("228-79", "zhe,nie");
        putMultiPinyin("228-84", "xian,kuo,tian,gua");
        putMultiPinyin("228-85", "hong,gong");
        putMultiPinyin("228-86", "zhong,yong");
        putMultiPinyin("228-87", "tou,tu,dou");
        putMultiPinyin("228-89", "mei,meng");
        putMultiPinyin("228-91", "wan,jian");
        putMultiPinyin("228-93", "yun,jun");
        putMultiPinyin("228-98", "ting,ding");
        putMultiPinyin("228-103", "juan,jian,cuan");
        putMultiPinyin("228-109", "xuan,juan");
        putMultiPinyin("228-110", "hua,wu");
        putMultiPinyin("228-114", "zhuo,chuo");
        putMultiPinyin("228-116", "xing,jing");
        putMultiPinyin("228-142", "zui,nie");
        putMultiPinyin("228-145", "yuan,wan");
        putMultiPinyin("228-171", "kuai,hui");
        putMultiPinyin("228-176", "hu,xu");
        putMultiPinyin("228-194", "du,dou");
        putMultiPinyin("228-196", "pi,pei");
        putMultiPinyin("228-197", "mian,sheng");
        putMultiPinyin("228-206", "yan,yin");
        putMultiPinyin("228-208", "qiu,jiao");
        putMultiPinyin("228-218", "zhen,qin");
        putMultiPinyin("228-234", "huang,guang");
        putMultiPinyin("228-240", "luo,ta");
        putMultiPinyin("228-248", "shu,zhu");
    }

    private static void initMap229() {
        putMultiPinyin("229-163", "dan,tan");
        putMultiPinyin("229-168", "bi,pi");
        putMultiPinyin("229-170", "zhuo,zhao");
        putMultiPinyin("229-181", "mi,fu");
        putMultiPinyin("229-238", "chan,can");
        putMultiPinyin("229-248", "che,cao");
        putMultiPinyin("229-250", "fei,pei");
    }

    private static void initMap230() {
        putMultiPinyin("230-92", "cuo,cha");
        putMultiPinyin("230-93", "da,ta");
        putMultiPinyin("230-97", "suo,se");
        putMultiPinyin("230-99", "yao,zu");
        putMultiPinyin("230-100", "ye,ta,ge");
        putMultiPinyin("230-106", "qiang,cheng");
        putMultiPinyin("230-107", "ge,li");
        putMultiPinyin("230-113", "bi,pi");
        putMultiPinyin("230-126", "wan,jian");
        putMultiPinyin("230-128", "gao,hao");
        putMultiPinyin("230-151", "zu,chuo");
        putMultiPinyin("230-157", "shou,sou");
        putMultiPinyin("230-175", "jiao,xiao");
        putMultiPinyin("230-193", "ao,yun,wo");
        putMultiPinyin("230-244", "piao,biao");
        putMultiPinyin("230-252", "he,ge");
    }

    private static void initMap231() {
        putMultiPinyin("231-68", "san,qiao,can");
        putMultiPinyin("231-71", "lu,ao");
        putMultiPinyin("231-90", "jian,zan");
        putMultiPinyin("231-105", "hui,sui,rui");
        putMultiPinyin("231-111", "san,xian,sa");
        putMultiPinyin("231-162", "pi,bi");
        putMultiPinyin("231-194", "bian,pian");
        putMultiPinyin("231-209", "mou,miu,miao,mu,liao");
        putMultiPinyin("231-216", "qiao,sao");
        putMultiPinyin("231-222", "zai,zi");
        putMultiPinyin("231-227", "bin,fen");
        putMultiPinyin("231-228", "min,wen");
        putMultiPinyin("231-245", "hun,hui");
    }

    private static void initMap232() {
        putMultiPinyin("232-149", "sa,xi");
        putMultiPinyin("232-157", "xian,kuo,tian,gua");
        putMultiPinyin("232-185", "yun,wen");
        putMultiPinyin("232-188", "shao,biao");
        putMultiPinyin("232-200", "cong,zong");
        putMultiPinyin("232-202", "fang,bing");
        putMultiPinyin("232-219", "ju,gou");
        putMultiPinyin("232-221", "li,yue");
        putMultiPinyin("232-222", "tuo,duo");
        putMultiPinyin("232-233", "gua,tian");
        putMultiPinyin("232-236", "heng,hang");
        putMultiPinyin("232-237", "gui,hui");
        putMultiPinyin("232-254", "zhao,zhuo");
    }

    private static void initMap233() {
        putMultiPinyin("233-88", "huo,shan");
        putMultiPinyin("233-92", "han,bi");
        putMultiPinyin("233-94", "ci ka Bi lu");
        putMultiPinyin("233-102", "xian,jian");
        putMultiPinyin("233-112", "xia,ke");
        putMultiPinyin("233-114", "bian,guan");
        putMultiPinyin("233-123", "hong,xiang");
        putMultiPinyin("233-145", "e,yan");
        putMultiPinyin("233-151", "hong,juan,xiang");
        putMultiPinyin("233-155", "ban,pan");
        putMultiPinyin("233-166", "di,dai,ti");
        putMultiPinyin("233-168", "cou,zou");
        putMultiPinyin("233-169", "zhen,shen");
        putMultiPinyin("233-171", "zha,cha");
        putMultiPinyin("233-196", "bin,bing");
        putMultiPinyin("233-202", "qi,se");
    }

    private static void initMap234() {
        putMultiPinyin("234-67", "pBi ying,po he deng");
        putMultiPinyin("234-79", "tang,chang");
        putMultiPinyin("234-82", "kan,han");
        putMultiPinyin("234-83", "xi,se,ta");
        putMultiPinyin("234-92", "han,bi");
        putMultiPinyin("234-156", "yu,yao,shu");
        putMultiPinyin("234-160", "dui,zhui");
        putMultiPinyin("234-176", "zang,cang");
        putMultiPinyin("234-186", "gan,han");
        putMultiPinyin("234-193", "jiong,gui");
        putMultiPinyin("234-200", "qi,shi");
        putMultiPinyin("234-201", "sheng,cheng");
        putMultiPinyin("234-249", "jian,qian");
        putMultiPinyin("234-253", "suo,sB,shB");
    }

    private static void initMap235() {
        putMultiPinyin("235-66", "qi,gai,ai");
        putMultiPinyin("235-68", "hui,duo");
        putMultiPinyin("235-84", "ao,yu");
        putMultiPinyin("235-95", "li,dai");
        putMultiPinyin("235-96", "li,dai");
        putMultiPinyin("235-97", "hu,he");
        putMultiPinyin("235-104", "jun,juan");
        putMultiPinyin("235-113", "guan,huan");
        putMultiPinyin("235-118", "gui,xi");
        putMultiPinyin("235-121", "nan,nuo");
        putMultiPinyin("235-129", "se,xi");
        putMultiPinyin("235-137", "wu,meng");
        putMultiPinyin("235-162", "bo,bai");
        putMultiPinyin("235-192", "rong,chen");
        putMultiPinyin("235-198", "zhun,chun");
        putMultiPinyin("235-212", "qu,xu,chun");
        putMultiPinyin("235-254", "shan,dan");
    }

    private static void initMap236() {
        putMultiPinyin("236-145", "ge,ta,sa");
        putMultiPinyin("236-147", "jie,ji");
        putMultiPinyin("236-153", "bian,ying");
        putMultiPinyin("236-156", "xuan,juan");
        putMultiPinyin("236-160", "shang,zhang");
        putMultiPinyin("236-168", "xi,she");
        putMultiPinyin("236-182", "yu,wu");
        putMultiPinyin("236-204", "zhuo,chao");
        putMultiPinyin("236-217", "yun,yu");
        putMultiPinyin("236-225", "huo,biao");
        putMultiPinyin("236-248", "chan,shan");
    }

    private static void initMap237() {
        putMultiPinyin("237-64", "bing,pi,bi,bei");
        putMultiPinyin("237-67", "xie,die");
        putMultiPinyin("237-74", "mu,mou");
        putMultiPinyin("237-77", "wen,yun");
        putMultiPinyin("237-83", "bi,bing");
        putMultiPinyin("237-105", "mei,wa");
        putMultiPinyin("237-115", "she,xie");
        putMultiPinyin("237-165", "nen,nin");
        putMultiPinyin("237-176", "gang,zhuang");
        putMultiPinyin("237-179", "ta,da");
        putMultiPinyin("237-185", "xu,hua");
        putMultiPinyin("237-199", "li,la");
        putMultiPinyin("237-201", "fu,fei");
        putMultiPinyin("237-209", "luo,ge");
        putMultiPinyin("237-217", "jie,ya");
        putMultiPinyin("237-244", "yi,chi");
        putMultiPinyin("237-245", "gui,sui");
    }

    private static void initMap238() {
        putMultiPinyin("238-174", "ting,ding");
        putMultiPinyin("238-217", "ba,pa");
        putMultiPinyin("238-228", "dian,tian");
        putMultiPinyin("238-232", "ta,tuo");
        putMultiPinyin("238-245", "dang,cheng");
        putMultiPinyin("238-250", "ting,ding");
        putMultiPinyin("238-254", "ha,ke");
    }

    private static void initMap239() {
        putMultiPinyin("239-77", "biao,diu");
        putMultiPinyin("239-84", "ba,fu");
        putMultiPinyin("239-98", "sao,sou");
        putMultiPinyin("239-102", "liu,liao");
        putMultiPinyin("239-133", "yang,juan");
        putMultiPinyin("239-140", "zhu,tou");
        putMultiPinyin("239-142", "zuo,ze,zha");
        putMultiPinyin("239-162", "diao,tiao,yao");
    }

    private static void initMap240() {
        putMultiPinyin("240-221", "li,lai");
        putMultiPinyin("240-251", "chai,cuo");
        putMultiPinyin("240-253", "jia,xia");
    }

    private static void initMap241() {
        putMultiPinyin("241-78", "yun,wo");
        putMultiPinyin("241-84", "feng,ping");
        putMultiPinyin("241-87", "tuo,duo");
        putMultiPinyin("241-88", "tuo,zhe");
        putMultiPinyin("241-92", "zhi,shi");
        putMultiPinyin("241-94", "xin,jin");
        putMultiPinyin("241-105", "jue,kuai");
        putMultiPinyin("241-106", "tuo,duo");
        putMultiPinyin("241-126", "tai,dai");
        putMultiPinyin("241-143", "xun,xuan");
        putMultiPinyin("241-187", "tiao,yao");
        putMultiPinyin("241-191", "yin,xun");
        putMultiPinyin("241-202", "jia,jie,qia");
        putMultiPinyin("241-211", "xi,ti");
        putMultiPinyin("241-212", "bi,pi");
        putMultiPinyin("241-226", "pi,ya,shu");
        putMultiPinyin("241-230", "jin,qin,guan");
        putMultiPinyin("241-251", "tan,qin");
    }

    private static void initMap242() {
        putMultiPinyin("242-64", "liang,lang");
        putMultiPinyin("242-161", "jie,xie,jia");
        putMultiPinyin("242-162", "he,ge");
        putMultiPinyin("242-188", "gong,zhong");
        putMultiPinyin("242-254", "mang,meng");
    }

    private static void initMap243() {
        putMultiPinyin("243-99", "bei,mo");
        putMultiPinyin("243-102", "qiao,xiao");
        putMultiPinyin("243-112", "bo,jue");
        putMultiPinyin("243-143", "bi,po");
        putMultiPinyin("243-144", "mao,meng");
        putMultiPinyin("243-150", "kuo,yue");
        putMultiPinyin("243-167", "shi,zhe");
        putMultiPinyin("243-195", "zhu,du");
        putMultiPinyin("243-208", "zuo,ze");
        putMultiPinyin("243-222", "yun,jun");
        putMultiPinyin("243-228", "qing,jing");
        putMultiPinyin("243-238", "wan,yuan");
    }

    private static void initMap244() {
        putMultiPinyin("244-210", "zi,ci");
        putMultiPinyin("244-214", "san,shen");
        putMultiPinyin("244-233", "mi,si");
        putMultiPinyin("244-236", "qing,qi");
        putMultiPinyin("244-237", "yao,you,zhou");
        putMultiPinyin("244-242", "qie,ju");
    }

    private static void initMap245() {
        putMultiPinyin("245-74", "ci,ji");
        putMultiPinyin("245-78", "bo,ba");
        putMultiPinyin("245-105", "luo,ge");
        putMultiPinyin("245-113", "gui,xie,wa,kui");
        putMultiPinyin("245-139", "pu,bu");
        putMultiPinyin("245-192", "bao,bo");
        putMultiPinyin("245-200", "li,luo");
        putMultiPinyin("245-232", "qi,xi");
        putMultiPinyin("245-254", "zi,zui");
    }

    private static void initMap246() {
        putMultiPinyin("246-71", "yi,si");
        putMultiPinyin("246-149", "ha ta ha ta");
        putMultiPinyin("246-184", "yin,ken");
        putMultiPinyin("246-188", "min,mian,meng");
        putMultiPinyin("246-191", "zhui,cui,wei");
        putMultiPinyin("246-193", "jun,juan");
        putMultiPinyin("246-196", "qu,ju");
        putMultiPinyin("246-217", "gui,xie");
    }

    private static void initMap247() {
        putMultiPinyin("247-133", "he,ge");
        putMultiPinyin("247-136", "bo,ba");
        putMultiPinyin("247-172", "gui,jue");
        putMultiPinyin("247-180", "man,men");
        putMultiPinyin("247-225", "mo,me");
        putMultiPinyin("247-229", "jun,qun");
    }

    private static void initMap248() {
        putMultiPinyin("248-64", "zhan,shan");
        putMultiPinyin("248-66", "niao,diao");
        putMultiPinyin("248-74", "diao,zhao");
        putMultiPinyin("248-78", "gan,han,yan");
        putMultiPinyin("248-87", "fu,gui");
        putMultiPinyin("248-88", "ban,fen");
        putMultiPinyin("248-90", "jian,qian,zhan");
    }

    private static void initMap249() {
        putMultiPinyin("249-149", "ti,chi");
        putMultiPinyin("249-151", "ti,chi");
        putMultiPinyin("249-155", "fu,bi");
        putMultiPinyin("249-159", "he,jie");
    }

    private static void initMap250() {
        putMultiPinyin("250-64", "pian,bian");
        putMultiPinyin("250-69", "chuan,zhi");
        putMultiPinyin("250-73", "cang,qiang");
        putMultiPinyin("250-75", "he,hu");
        putMultiPinyin("250-88", "gu,hu");
        putMultiPinyin("250-90", "sun,xun");
        putMultiPinyin("250-121", "lou,lv");
    }

    private static void initMap251() {
        putMultiPinyin("251-129", "pao,biao");
        putMultiPinyin("251-132", "zhu,cu");
    }

    private static void initMap252() {
        putMultiPinyin("252-78", "mo,me");
        putMultiPinyin("252-108", "dan,shen");
        putMultiPinyin("252-109", "zhen,yan");
        putMultiPinyin("252-114", "dan,zhan");
        putMultiPinyin("252-119", "min,mian,meng");
    }

    private static void initMap253() {
        putMultiPinyin("253-135", "yin,ken");
        putMultiPinyin("253-138", "gong,wo");
        putMultiPinyin("253-148", "gui,jun,qiu");
    }

    private static String getCharacterGbk(char character) {
        try {
            byte[] bytes = String.valueOf(character).getBytes("GBK");
            if (bytes == null) {
                return "";
            }
            switch (bytes.length) {
                case 1:
                    return new String(bytes);
                case 2:
                    return (bytes[0] + 256) + "-" + (bytes[1] + 256);
                default:
                    return "";
            }
            e.printStackTrace();
            return "";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String getMultiPinyinsByGBK(String keyGbk) {
        if (keyGbk.indexOf(45) <= -1) {
            return keyGbk;
        }
        multiPinyinMap.clear();
        initHashMap(keyGbk);
        return (String) multiPinyinMap.get(keyGbk);
    }

    private void initHashMap(String keyGbk) {
        int highByteInt;
        try {
            highByteInt = Integer.parseInt(keyGbk.substring(0, keyGbk.indexOf("-")));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            highByteInt = -1;
        }
        initHashMapFragment(highByteInt);
    }

    private void initHashMapFragment(int highByteInt) {
        switch (highByteInt) {
            case 129:
                initMap129();
                return;
            case 130:
                initMap130();
                return;
            case 131:
                initMap131();
                return;
            case 132:
                initMap132();
                return;
            case 133:
                initMap133();
                return;
            case 134:
                initMap134();
                return;
            case 135:
                initMap135();
                return;
            case 136:
                initMap136();
                return;
            case 137:
                initMap137();
                return;
            case 138:
                initMap138();
                return;
            case 139:
                initMap139();
                return;
            case 140:
                initMap140();
                return;
            case 141:
                initMap141();
                return;
            case 142:
                initMap142();
                return;
            case 143:
                initMap143();
                return;
            case 144:
                initMap144();
                return;
            case 145:
                initMap145();
                return;
            case 146:
                initMap146();
                return;
            case 147:
                initMap147();
                return;
            case 148:
                initMap148();
                return;
            case 149:
                initMap149();
                return;
            case 150:
                initMap150();
                return;
            case 151:
                initMap151();
                return;
            case 152:
                initMap152();
                return;
            case 153:
                initMap153();
                return;
            case 154:
                initMap154();
                return;
            case 155:
                initMap155();
                return;
            case 156:
                initMap156();
                return;
            case 157:
                initMap157();
                return;
            case 158:
                initMap158();
                return;
            case 159:
                initMap159();
                return;
            case 160:
                initMap160();
                return;
            case 170:
                initMap170();
                return;
            case 171:
                initMap171();
                return;
            case 172:
                initMap172();
                return;
            case 173:
                initMap173();
                return;
            case 174:
                initMap174();
                return;
            case 175:
                initMap175();
                return;
            case 176:
                initMap176();
                return;
            case 177:
                initMap177();
                return;
            case 178:
                initMap178();
                return;
            case 179:
                initMap179();
                return;
            case 180:
                initMap180();
                return;
            case 181:
                initMap181();
                return;
            case 182:
                initMap182();
                return;
            case 183:
                initMap183();
                return;
            case 184:
                initMap184();
                return;
            case 185:
                initMap185();
                return;
            case 186:
                initMap186();
                return;
            case 187:
                initMap187();
                return;
            case 188:
                initMap188();
                return;
            case 189:
                initMap189();
                return;
            case 190:
                initMap190();
                return;
            case 191:
                initMap191();
                return;
            case 192:
                initMap192();
                return;
            case 193:
                initMap193();
                return;
            case 194:
                initMap194();
                return;
            case 195:
                initMap195();
                return;
            case 196:
                initMap196();
                return;
            case 197:
                initMap197();
                return;
            case 198:
                initMap198();
                return;
            case 199:
                initMap199();
                return;
            case 200:
                initMap200();
                return;
            case 201:
                initMap201();
                return;
            case 202:
                initMap202();
                return;
            case 203:
                initMap203();
                return;
            case 204:
                initMap204();
                return;
            case 205:
                initMap205();
                return;
            case 206:
                initMap206();
                return;
            case 207:
                initMap207();
                return;
            case 208:
                initMap208();
                return;
            case 209:
                initMap209();
                return;
            case 210:
                initMap210();
                return;
            case 211:
                initMap211();
                return;
            case 212:
                initMap212();
                return;
            case 213:
                initMap213();
                return;
            case 214:
                initMap214();
                return;
            case 215:
                initMap215();
                return;
            case 216:
                initMap216();
                return;
            case 217:
                initMap217();
                return;
            case 218:
                initMap218();
                return;
            case 219:
                initMap219();
                return;
            case 220:
                initMap220();
                return;
            case 221:
                initMap221();
                return;
            case 222:
                initMap222();
                return;
            case 223:
                initMap223();
                return;
            case 224:
                initMap224();
                return;
            case 225:
                initMap225();
                return;
            case 226:
                initMap226();
                return;
            case 227:
                initMap227();
                return;
            case 228:
                initMap228();
                return;
            case 229:
                initMap229();
                return;
            case 230:
                initMap230();
                return;
            case 231:
                initMap231();
                return;
            case 232:
                initMap232();
                return;
            case 233:
                initMap233();
                return;
            case 234:
                initMap234();
                return;
            case 235:
                initMap235();
                return;
            case 236:
                initMap236();
                return;
            case 237:
                initMap237();
                return;
            case 238:
                initMap238();
                return;
            case 239:
                initMap239();
                return;
            case 240:
                initMap240();
                return;
            case 241:
                initMap241();
                return;
            case 242:
                initMap242();
                return;
            case 243:
                initMap243();
                return;
            case 244:
                initMap244();
                return;
            case 245:
                initMap245();
                return;
            case 246:
                initMap246();
                return;
            case 247:
                initMap247();
                return;
            case 248:
                initMap248();
                return;
            case 249:
                initMap249();
                return;
            case Callback.DEFAULT_SWIPE_ANIMATION_DURATION /*250*/:
                initMap250();
                return;
            case 251:
                initMap251();
                return;
            case 252:
                initMap252();
                return;
            case 253:
                initMap253();
                return;
            default:
                return;
        }
    }

    public ArrayList<Token> get(String cnStr) {
        ArrayList<Token> tokens = new ArrayList();
        if (!(cnStr == null || "".equals(cnStr.trim()))) {
            for (char aChar : cnStr.toCharArray()) {
                if (aChar != ' ') {
                    String keyGbk = getCharacterGbk(aChar);
                    if (keyGbk.length() == 0) {
                        tokens.add(HanziToPinyin.getInstance().getTokenIncludingSpecialSuffix(aChar));
                    } else {
                        String pinyin = getMultiPinyinsByGBK(keyGbk);
                        if (pinyin == null) {
                            tokens.add(HanziToPinyin.getInstance().getTokenIncludingSpecialSuffix(aChar));
                        } else {
                            Token token = new Token();
                            token.type = 2;
                            token.source = Character.toString(aChar);
                            token.target = pinyin.toUpperCase();
                            tokens.add(token);
                        }
                    }
                }
            }
        }
        return tokens;
    }

    public static HanziToMultiPinyin getInstance() {
        HanziToMultiPinyin hanziToMultiPinyin;
        synchronized (HanziToMultiPinyin.class) {
            if (sInstance == null) {
                sInstance = new HanziToMultiPinyin();
            }
            hanziToMultiPinyin = sInstance;
        }
        return hanziToMultiPinyin;
    }
}
