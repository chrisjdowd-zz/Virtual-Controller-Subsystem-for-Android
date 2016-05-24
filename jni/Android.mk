LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog
LOCAL_MODULE    := EventEmulator
LOCAL_SRC_FILES := EventEmulator.c
include $(BUILD_SHARED_LIBRARY)
