LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := aquestalk2
LOCAL_SRC_FILES := libAquesTalk2.so
include $(PREBUILT_SHARED_LIBRARY)