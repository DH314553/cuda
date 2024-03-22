public class NativeLibrary {
    
    // Kotlin��JNI�֐����`����
    public static native void cudaRidgeDetection(float[] data, float[] count, int rows, int cols, float thres);
    
    // Native���C�u���������[�h����
    static {
        System.loadLibrary("libcuda_ridge_detection");
    }
    
    // JNI�l�C�e�B�u���\�b�h���`����
    public static native void cudaRidgeDetectionJNI(float[] data, float[] count, int rows, int cols, float thres);
}
