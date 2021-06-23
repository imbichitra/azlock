LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := ndklink
LOCAL_SRC_FILES := ndklink.c

include $(BUILD_SHARED_LIBRARY)