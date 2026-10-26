cmake_minimum_required(VERSION 3.22.1)
project(vision_native LANGUAGES CXX)

add_library(vision_native SHARED
    src/main/cpp/vision_native.cpp
)

target_compile_features(vision_native PRIVATE cxx_std_17)
target_compile_options(vision_native PRIVATE -Wall -Wextra -Werror)
