cmake_minimum_required(VERSION 3.22.1)
project(autoclicker_native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find OpenCV from prefab package provided by AAR
find_package(OpenCV REQUIRED COMPONENTS OpenCV::opencv_java4)

add_library(autoclicker_native SHARED match_template.cpp)

target_link_libraries(autoclicker_native
        OpenCV::opencv_java4
        android
        log)