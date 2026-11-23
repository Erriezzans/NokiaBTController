cmake_minimum_required(VERSION 3.22.1)
project(nrgpad C)

add_executable(nrgpad nrgpad.c)
target_compile_options(nrgpad PRIVATE -Wall -Wextra -Werror=implicit-function-declaration -fPIE)
target_link_options(nrgpad PRIVATE -pie)

# Place the Android executable in the matching APK asset directory.
set(ABI_ASSET_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../assets/bin/${ANDROID_ABI}")
file(MAKE_DIRECTORY "${ABI_ASSET_DIR}")
set_target_properties(nrgpad PROPERTIES RUNTIME_OUTPUT_DIRECTORY "${ABI_ASSET_DIR}")
