#include<jni.h>
JNIEXPORT jstring  JNICALL Java_com_asiczen_azlibrary_Utils_getInfo(JNIEnv* env,jobject obj){
    return (*env)->NewStringUTF(env,"2B7E151628AED2A6ABF7158809CF4F3C");
}
