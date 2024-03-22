public class NativeLibrary {
    
    // KotlinのJNI関数を定義する
    public static native void cudaRidgeDetection(float[] data, float[] count, int rows, int cols, float thres);
    
    // Nativeライブラリをロードする
    static {
        System.loadLibrary("libcuda_ridge_detection");
    }
    
    // JNIネイティブメソッドを定義する
    public static native void cudaRidgeDetectionJNI(float[] data, float[] count, int rows, int cols, float thres);
}
