#include <jni.h>
#include <opencv2/opencv.hpp>

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_mycompany_autoclicker_cv_CvTemplateMatcher_matchTemplateNative(JNIEnv *env, jclass /*clazz*/,
                                                                        jlong srcMatPtr,
                                                                        jlong templMatPtr) {
    auto &src = *reinterpret_cast<cv::Mat *>(srcMatPtr);
    auto &templ = *reinterpret_cast<cv::Mat *>(templMatPtr);

    if (src.empty() || templ.empty()) {
        jfloatArray err = env->NewFloatArray(3);
        float zeros[3] = {0.f, 0.f, 0.f};
        env->SetFloatArrayRegion(err, 0, 3, zeros);
        return err;
    }

    cv::Mat result;
    cv::matchTemplate(src, templ, result, cv::TM_CCOEFF_NORMED);
    double minVal, maxVal;
    cv::Point minLoc, maxLoc;
    cv::minMaxLoc(result, &minVal, &maxVal, &minLoc, &maxLoc);

    jfloat out[3];
    out[0] = static_cast<float>(maxVal);
    out[1] = static_cast<float>(maxLoc.x);
    out[2] = static_cast<float>(maxLoc.y);

    jfloatArray res = env->NewFloatArray(3);
    env->SetFloatArrayRegion(res, 0, 3, out);
    return res;
}