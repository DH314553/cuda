#include <cuda_runtime.h>
#include <stdio.h>

__global__ void cuda_ridge_detection_kernel(float *data, float *count, int rows, int cols, float thres) {
    int i = blockIdx.y * blockDim.y + threadIdx.y;
    int j = blockIdx.x * blockDim.x + threadIdx.x;

    if (i > 0 && j > 0 && i < rows - 1 && j < cols - 1) {
        if (data[i * cols + j] > thres && !isnan(data[i * cols + j])) {
            int step_i = i;
            int step_j = j;
            for (int k = 0; k < 1000; k++) {
                if (step_i == 0 || step_j == 0 || step_i == rows - 1 || step_j == cols - 1) {
                    break;
                }
                int index = 4;
                float vmax = -INFINITY;
                for (int ii = 0; ii < 3; ii++) {
                    for (int jj = 0; jj < 3; jj++) {
                        float value = data[(step_i + ii - 1) * cols + step_j + jj - 1];
                        if (value > vmax) {
                            vmax = value;
                            index = jj + 3 * ii;
                        }
                    }
                }
                if (index == 4 || vmax == data[step_i * cols + step_j] || isnan(vmax)) {
                    break;
                }
                int row = index / 3;
                int col = index % 3;
                atomicAdd(&count[(step_i - 1 + row) * cols + step_j - 1 + col], 1.0f);
                step_i = step_i - 1 + row;
                step_j = step_j - 1 + col;
            }
        }
    }
}