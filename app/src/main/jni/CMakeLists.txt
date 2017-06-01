# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.6)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add.library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

# Add respeck-jni.c and all files in breathing and activitiyclassification directories to library sources
file(GLOB RESPECK_SOURCES respeck-jni.c respeck.c respeck.h breathing/* activityclassification/*)

add_library( # Specifies the name of the library.
             respeck-jni

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             ${RESPECK_SOURCES})

find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.
              log-lib

              # Specifies the name of the NDK library that
              # CMake needs to locate.
              log )

# Links your native library against one or more other native libraries.
target_link_libraries( # Specifies the target library.
                       respeck-jni

                       # Links the log library to the target library.
                       ${log-lib} )