# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

## CUDAのバージョン指定と必要なライブラリの検出
#find_package(CUDA 12.4.99 REQUIRED)

# CUDAコンパイラと関連するパスの設定
set(CUDA_VERSION 12.4.99)
set(CMAKE_CUDA_COMPILER "/usr/local/cuda/bin/nvcc")
set(CUDA_TOOLKIT_ROOT_DIR "/usr/local/cuda")

# C++およびCUDAのソースファイルをリストアップ
set(SOURCES
        NativeLibrary.cu
        cuda_ridge_detection_kernel.cu
)

# CUDA言語を有効にする
#enable_language(CUDA)

# プロジェクト名を指定し、CUDAを使用することを宣言
#project(cuda_project CUDA)
#
## ターゲットライブラリの作成
#add_library(cuda_library SHARED ${SOURCES})
#
## ターゲットプロパティの設定
#set_target_properties(cuda_library PROPERTIES LINKER_LANGUAGE CUDA)
#
## CUDA_INCLUDE_DIRSとCUDA_CUDART_LIBRARYは既に定義されているものとします
#target_include_directories(cuda_library PRIVATE ${CUDA_INCLUDE_DIRS})
#target_link_libraries(cuda_library PRIVATE ${CUDA_CUDART_LIBRARY})
