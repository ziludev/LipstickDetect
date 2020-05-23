#include <jni.h>
#include <string>
#include <sstream>
#include "com_example_lipstickdetect_MainActivity.h"
#include "com_example_lipstickdetect_FaceDetect.h"
#include <android/bitmap.h>
#include "opencv2/opencv.hpp"
#include "opencv2/core.hpp"
#include "opencv2/core/base.hpp"
#include "opencv2/core/hal/interface.h"
#include "opencv2/imgproc.hpp"
#include "opencv2/core/cvdef.h"
#include <android/asset_manager_jni.h>

using namespace cv;


struct membuf : std::streambuf {
    membuf(char *begin, char *end) {
        this->setg(begin, begin, end);
    }
};

shape_predictor pose_model;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_lipstickdetect_FaceDetect_loadShapePredictor(JNIEnv *env, jclass type,
                                                              jobject asset_manager) {
    // TODO: implement loadShapePredictor()
//    const char *fileName = env->GetStringUTFChars(file_name, nullptr);
//    env->ReleaseStringUTFChars(file_name, fileName);

    // 获取assetManager
    AAssetManager *native_asset = AAssetManager_fromJava(env, asset_manager);

    // 打开文件
    AAsset *assetFile = AAssetManager_open(native_asset, "shape_predictor_68_face_landmarks.dat", AASSET_MODE_BUFFER);
    // 获取文件大小
    size_t file_length = static_cast<size_t>(AAsset_getLength(assetFile));
    char *model_buffer = (char *) malloc(file_length);
    // 读取文件数据
    AAsset_read(assetFile, model_buffer, file_length);
    // 获取到文件后关闭文件
    AAsset_close(assetFile);

    // char变成iosstream
    membuf mem_buf(model_buffer, model_buffer + file_length);
    std::istream in(&mem_buf);

    deserialize(pose_model, in);
    // 释放内存
    free(model_buffer);
}

/*
 * Class:     com_example_lipstickdetect_FaceDetect
 * Method:    LandmarkDetection
 * Signature: (Ljava/lang/Object;)V
 */
extern "C" JNIEXPORT jstring JNICALL Java_com_example_lipstickdetect_FaceDetect_LandmarkDetection
        (JNIEnv *env, jclass obj, jlong addrInput, jlong addrOutput) {

    // 获取传入的参数，进行强转
    Mat &image = *(Mat *) addrInput;
    Mat &dst = *(Mat *) addrOutput;
    const char *strContent;
    std::string RGBStr;

    try {
        // 获取检测器
        frontal_face_detector detector = get_frontal_face_detector();

//        deserialize("/sdcard/facelandmark/shape_predictor_68_face_landmarks.dat") >> pose_model;

        // 将opencv图像转化为dlib图像
        cv_image<bgr_pixel> cimg(image);
        // 检测，面部信息存储在faces中
        std::vector<dlib::rectangle> faces = detector(cimg);
        std::vector<full_object_detection> shapes;

        // 存储面部的landmarks信息
        for (unsigned long i = 0; i < faces.size(); i++) {
            shapes.push_back(pose_model(cimg, faces[i]));
        }
        // 将结果赋值到dst对象内
        dst = image.clone();

        // 将landmarks进行绘图，以便观看显示
        renderToMat(shapes, dst);

        // 截取口红图片
        cv::Mat img;
        cv::Rect rect;
        for (unsigned long idx = 0; idx < shapes.size(); idx++) {
            rect = cv::Rect(shapes[idx].part(61).x(), shapes[idx].part(61).y(), 20, 10);
            img = dst(rect);
        }
        int x = 10;
        int y = 10;
        int range = 10;
        int R_sum = 0;
        int G_sum = 0;
        int B_sum = 0;
        for (int i = 0; i < range; i++) {
            for (int j = 0; j < range; j++) {
                Vec3b &p = img.at<Vec3b>(y - range / 2 + i, x - range / 2 + j);
                // 求RGB的均值
                R_sum += p[2];
                G_sum += p[1];
                B_sum += p[0];
            }
        }

        RGBStr = rgb2hex(R_sum/100,G_sum/100,B_sum/100);

//        libstickDetect(shapes,dst);

    } catch (serialization_error &e) {
        cout << endl << e.what() << endl;
    }
    strContent = (char *)RGBStr.data();
    return env->NewStringUTF(strContent);
}

void renderToMat(std::vector<full_object_detection> &dets, Mat &dst) {
    Scalar color;
    int sz = 3;
    color = Scalar(0, 255, 0);
    //chin line
    for (unsigned long idx = 0; idx < dets.size(); idx++) {
        for (unsigned long i = 1; i <= 16; ++i)
            cv::line(dst, Point(dets[idx].part(i).x(), dets[idx].part(i).y()),
                     Point(dets[idx].part(i - 1).x(), dets[idx].part(i - 1).y()), color, sz);
        //line on top of nose
        for (unsigned long i = 28; i <= 30; ++i)
            cv::line(dst, Point(dets[idx].part(i).x(), dets[idx].part(i).y()),
                     Point(dets[idx].part(i - 1).x(), dets[idx].part(i - 1).y()), color, sz);
        //left eyebrow
        for (unsigned long i = 18; i <= 21; ++i)
            cv::line(dst, Point(dets[idx].part(i).x(), dets[idx].part(i).y()),
                     Point(dets[idx].part(i - 1).x(), dets[idx].part(i - 1).y()), color, sz);
        //right eyebrow
        for (unsigned long i = 23; i <= 26; ++i)
            cv::line(dst, Point(dets[idx].part(i).x(), dets[idx].part(i).y()),
                     Point(dets[idx].part(i - 1).x(), dets[idx].part(i - 1).y()), color, sz);
        //bottom of nose
        for (unsigned long i = 31; i <= 35; ++i)
            cv::line(dst, Point(dets[idx].part(i).x(), dets[idx].part(i).y()),
                     Point(dets[idx].part(i - 1).x(), dets[idx].part(i - 1).y()), color, sz);
        cv::line(dst, Point(dets[idx].part(30).x(), dets[idx].part(30).y()),
                 Point(dets[idx].part(35).x(), dets[idx].part(35).y()), color, sz);
        //left eye
        for (unsigned long i = 37; i <= 41; ++i)
            cv::line(dst, Point(dets[idx].part(i).x(), dets[idx].part(i).y()),
                     Point(dets[idx].part(i - 1).x(), dets[idx].part(i - 1).y()), color, sz);
        cv::line(dst, Point(dets[idx].part(36).x(), dets[idx].part(36).y()),
                 Point(dets[idx].part(41).x(), dets[idx].part(41).y()), color, sz);
        //right eye
        for (unsigned long i = 43; i <= 47; ++i)
            cv::line(dst, Point(dets[idx].part(i).x(), dets[idx].part(i).y()),
                     Point(dets[idx].part(i - 1).x(), dets[idx].part(i - 1).y()), color, sz);
        cv::line(dst, Point(dets[idx].part(42).x(), dets[idx].part(42).y()),
                 Point(dets[idx].part(47).x(), dets[idx].part(47).y()), color, sz);
        //lips out part
        for (unsigned long i = 49; i <= 59; ++i)
            cv::line(dst, Point(dets[idx].part(i).x(), dets[idx].part(i).y()),
                     Point(dets[idx].part(i - 1).x(), dets[idx].part(i - 1).y()), color, sz);
        cv::line(dst, Point(dets[idx].part(48).x(), dets[idx].part(48).y()),
                 Point(dets[idx].part(59).x(), dets[idx].part(59).y()), color, sz);
        //lips inside part
        for (unsigned long i = 61; i <= 67; ++i)
            cv::line(dst, Point(dets[idx].part(i).x(), dets[idx].part(i).y()),
                     Point(dets[idx].part(i - 1).x(), dets[idx].part(i - 1).y()), color, sz);
        cv::line(dst, Point(dets[idx].part(60).x(), dets[idx].part(60).y()),
                 Point(dets[idx].part(67).x(), dets[idx].part(67).y()), color, sz);
    }
}

// 转换成16进制
std::string rgb2hex(int r, int g, int b) {
    std::stringstream ss;
    if (true)
        ss << "#";
    ss << std::hex << (r << 16 | g << 8 | b);
    return ss.str();
}