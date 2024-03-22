#include <jni.h>
#include "NativeLibrary.h" // JNIインターフェースのヘッダーファイル
#include <cuda_runtime.h>

extern "C" {
__attribute__((unused)) JNIEXPORT void JNICALL Java_com_daisaku31469_cuda_viewmodel_WeatherViewModel_cudaRidgeDetection(JNIEnv *env, jobject obj, jfloatArray data, jfloatArray count, jint rows, jint cols, jfloat thres);

__attribute__((unused)) void JNICALL Java_com_daisaku31469_cuda_viewmodel_WeatherViewModel_cudaRidgeDetection(JNIEnv *env, jobject obj, jfloatArray data, jfloatArray count, jint rows, jint cols, jfloat thres) {
// JNIからJava配列を取得
jfloat *dataPtr = env->GetFloatArrayElements(data, NULL);
jfloat *countPtr = env->GetFloatArrayElements(count, NULL);

// CUDAのカーネル関数を呼び出す
cuda_ridge_detection_kernel<<<gridSize, blockSize>>>(dataPtr, countPtr, rows, cols, thres);

// CUDAの計算結果をホストメモリにコピー
cudaMemcpy(countPtr, countCuda, rows * cols * sizeof(float), cudaMemcpyDeviceToHost);

// JNIからJava配列への変更を反映
env->ReleaseFloatArrayElements(data, dataPtr, 0);
env->ReleaseFloatArrayElements(count, countPtr, 0);
}
}
