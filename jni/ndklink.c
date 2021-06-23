//
// Created by bpradhan on 30-04-2018.
//
#include<jni.h>
//68747470733a2f2f31332e3132372e3130392e31312f417a6c6f636b546573742f -> https://13.127.109.11/AzlockTest/
//68747470733a2f2f31332e3132372e3130392e31312f417a6c6f636b2f /*ipadress -> https://13.127.109.11/Azlock/*/
JNIEXPORT jobjectArray JNICALL Java_com_asiczen_azlock_MainActivity_getInfo(JNIEnv* env,jobject obj){
    char *days[]={"2B7E151628AED2A6ABF7158809CF4F3C",//AppKey
                  "2A7F151628AED2A6ABF7158809CF4F3C",//key
                  "5723252421405e262a28295f2b5137",//userid -> W#%$!@^&*()_+Q7
                  "5b28297b6c6f636b7d5d2f2d3435",//password -> [(){lock}]/-45
                  "68747470733a2f2f31332e3132372e3130392e31312f417a6c6f636b2f",/*ipadress*/
                  "6164646C6F636B2E706870", //addlock.php
                  "6368616E67655F70617373776F72642E706870", //change_password.php
                  "777269746570696E2E706870",  //writepin.php
                  "666F72676F745F70617373776F72642E706870", //forgot_password.php
                  "7265616470696E2E706870", //readpin.php
                  "6372656174652E706870", //create.php
                  "73656e645f6d61696c2e706870", ////send_mail.php
                  "646f625f766572696669636174696f6e2e706870", //dob_verification.php
                  "7365637265745f51315f766572696669636174696f6e2e706870", //secret_Q1_verification.php
                  "7365637265745f51325f766572696669636174696f6e2e706870",//secret_Q2_verification.php
                  "7365637265745f616e732e706870",//secret_ans.php
                  "706f73745f746f5f637573746f6d65725f696e666f2e706870",//post_to_customer_info.php
                  "6c6f67696e2e706870",//login.php
                  "61646d696e40617a6c6f636b2e696e", //admin@azlock.in
                  "72616973655f69737375652e706870" //raise_issue.php
                  };
    jstring str;
    jobjectArray day = 0;
    jsize len = 20;
    int i;
    day = (*env)->NewObjectArray(env,len,(*env)->FindClass(env,"java/lang/String"),0);

    for(i=0;i<20;i++)
    {
        str = (*env)->NewStringUTF(env,days[i]);
        (*env)->SetObjectArrayElement(env,day,i,str);
    }

    return day;
}


JNIEXPORT jobjectArray JNICALL
Java_com_asiczen_azlock_SlideViewActivity_getUrls(JNIEnv *env, jobject thiz) {
    char *days[]={"2B7E151628AED2A6ABF7158809CF4F3C",//AppKey
                  "2A7F151628AED2A6ABF7158809CF4F3C",//key
                  "5723252421405e262a28295f2b5137",//userid -> W#%$!@^&*()_+Q7
                  "5b28297b6c6f636b7d5d2f2d3435",//password -> [(){lock}]/-45
                  "68747470733a2f2f31332e3132372e3130392e31312f417a6c6f636b2f",/*ipadress*/
                  "6164646C6F636B2E706870", //addlock.php
                  "6368616E67655F70617373776F72642E706870", //change_password.php
                  "777269746570696E2E706870",  //writepin.php
                  "666F72676F745F70617373776F72642E706870", //forgot_password.php
                  "7265616470696E2E706870", //readpin.php
                  "6372656174652E706870", //create.php
                  "73656e645f6d61696c2e706870", ////send_mail.php
                  "646f625f766572696669636174696f6e2e706870", //dob_verification.php
                  "7365637265745f51315f766572696669636174696f6e2e706870", //secret_Q1_verification.php
                  "7365637265745f51325f766572696669636174696f6e2e706870",//secret_Q2_verification.php
                  "7365637265745f616e732e706870",//secret_ans.php
                  "706f73745f746f5f637573746f6d65725f696e666f2e706870",//post_to_customer_info.php
                  "6c6f67696e2e706870",//login.php
                  "61646d696e40617a6c6f636b2e696e", //admin@azlock.in
                  "72616973655f69737375652e706870" //raise_issue.php
    };
    jstring str;
    jobjectArray day = 0;
    jsize len = 20;
    int i;
    day = (*env)->NewObjectArray(env,len,(*env)->FindClass(env,"java/lang/String"),0);

    for(i=0;i<20;i++)
    {
        str = (*env)->NewStringUTF(env,days[i]);
        (*env)->SetObjectArrayElement(env,day,i,str);
    }

    return day;
}
