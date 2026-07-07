#ifndef KONAN_LIBSHARED_NATIVELIBS_H
#define KONAN_LIBSHARED_NATIVELIBS_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            libshared_nativeLibs_KBoolean;
#else
typedef _Bool           libshared_nativeLibs_KBoolean;
#endif
typedef unsigned short     libshared_nativeLibs_KChar;
typedef signed char        libshared_nativeLibs_KByte;
typedef short              libshared_nativeLibs_KShort;
typedef int                libshared_nativeLibs_KInt;
typedef long long          libshared_nativeLibs_KLong;
typedef unsigned char      libshared_nativeLibs_KUByte;
typedef unsigned short     libshared_nativeLibs_KUShort;
typedef unsigned int       libshared_nativeLibs_KUInt;
typedef unsigned long long libshared_nativeLibs_KULong;
typedef float              libshared_nativeLibs_KFloat;
typedef double             libshared_nativeLibs_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) libshared_nativeLibs_KVector128;
typedef void*              libshared_nativeLibs_KNativePtr;
struct libshared_nativeLibs_KType;
typedef struct libshared_nativeLibs_KType libshared_nativeLibs_KType;

typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_Byte;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_Short;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_Int;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_Long;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_Float;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_Double;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_Char;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_Boolean;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_Unit;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_UByte;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_UShort;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_UInt;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_ULong;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_com_basic_native_jvm_JvmBridge;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_com_basic_native_jvm__JvmBridgeNativeImpl;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_kotlin_ByteArray;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_com_basic_native_Encrypt;
typedef struct {
  libshared_nativeLibs_KNativePtr pinned;
} libshared_nativeLibs_kref_buildkonfig_BuildConfig_com_basic_nativeLibs;

extern void* Java_com_basic_native_utils_EncryptJni_createSign(void* env, void* clazz, void* p0, void* p1);
extern void* Java_com_basic_native_utils_EncryptJni_decodeData(void* env, void* clazz, void* p0, void* p1);
extern void* Java_com_basic_native_utils_EncryptJni_encodeData(void* env, void* clazz, void* p0, void* p1);

typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(libshared_nativeLibs_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  libshared_nativeLibs_KBoolean (*IsInstance)(libshared_nativeLibs_KNativePtr ref, const libshared_nativeLibs_KType* type);
  libshared_nativeLibs_kref_kotlin_Byte (*createNullableByte)(libshared_nativeLibs_KByte);
  libshared_nativeLibs_KByte (*getNonNullValueOfByte)(libshared_nativeLibs_kref_kotlin_Byte);
  libshared_nativeLibs_kref_kotlin_Short (*createNullableShort)(libshared_nativeLibs_KShort);
  libshared_nativeLibs_KShort (*getNonNullValueOfShort)(libshared_nativeLibs_kref_kotlin_Short);
  libshared_nativeLibs_kref_kotlin_Int (*createNullableInt)(libshared_nativeLibs_KInt);
  libshared_nativeLibs_KInt (*getNonNullValueOfInt)(libshared_nativeLibs_kref_kotlin_Int);
  libshared_nativeLibs_kref_kotlin_Long (*createNullableLong)(libshared_nativeLibs_KLong);
  libshared_nativeLibs_KLong (*getNonNullValueOfLong)(libshared_nativeLibs_kref_kotlin_Long);
  libshared_nativeLibs_kref_kotlin_Float (*createNullableFloat)(libshared_nativeLibs_KFloat);
  libshared_nativeLibs_KFloat (*getNonNullValueOfFloat)(libshared_nativeLibs_kref_kotlin_Float);
  libshared_nativeLibs_kref_kotlin_Double (*createNullableDouble)(libshared_nativeLibs_KDouble);
  libshared_nativeLibs_KDouble (*getNonNullValueOfDouble)(libshared_nativeLibs_kref_kotlin_Double);
  libshared_nativeLibs_kref_kotlin_Char (*createNullableChar)(libshared_nativeLibs_KChar);
  libshared_nativeLibs_KChar (*getNonNullValueOfChar)(libshared_nativeLibs_kref_kotlin_Char);
  libshared_nativeLibs_kref_kotlin_Boolean (*createNullableBoolean)(libshared_nativeLibs_KBoolean);
  libshared_nativeLibs_KBoolean (*getNonNullValueOfBoolean)(libshared_nativeLibs_kref_kotlin_Boolean);
  libshared_nativeLibs_kref_kotlin_Unit (*createNullableUnit)(void);
  libshared_nativeLibs_kref_kotlin_UByte (*createNullableUByte)(libshared_nativeLibs_KUByte);
  libshared_nativeLibs_KUByte (*getNonNullValueOfUByte)(libshared_nativeLibs_kref_kotlin_UByte);
  libshared_nativeLibs_kref_kotlin_UShort (*createNullableUShort)(libshared_nativeLibs_KUShort);
  libshared_nativeLibs_KUShort (*getNonNullValueOfUShort)(libshared_nativeLibs_kref_kotlin_UShort);
  libshared_nativeLibs_kref_kotlin_UInt (*createNullableUInt)(libshared_nativeLibs_KUInt);
  libshared_nativeLibs_KUInt (*getNonNullValueOfUInt)(libshared_nativeLibs_kref_kotlin_UInt);
  libshared_nativeLibs_kref_kotlin_ULong (*createNullableULong)(libshared_nativeLibs_KULong);
  libshared_nativeLibs_KULong (*getNonNullValueOfULong)(libshared_nativeLibs_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          struct {
            struct {
              struct {
                libshared_nativeLibs_KType* (*_type)(void);
                libshared_nativeLibs_kref_com_basic_native_jvm__JvmBridgeNativeImpl (*_JvmBridgeNativeImpl)(void* env, void* instance);
                const char* (*identify)(libshared_nativeLibs_kref_com_basic_native_jvm__JvmBridgeNativeImpl thiz);
                const char* (*process)(libshared_nativeLibs_kref_com_basic_native_jvm__JvmBridgeNativeImpl thiz, const char* source);
              } _JvmBridgeNativeImpl;
              struct {
                libshared_nativeLibs_KType* (*_type)(void);
                const char* (*identify)(libshared_nativeLibs_kref_com_basic_native_jvm_JvmBridge thiz);
                const char* (*process)(libshared_nativeLibs_kref_com_basic_native_jvm_JvmBridge thiz, const char* source);
              } JvmBridge;
            } jvm;
            struct {
              libshared_nativeLibs_kref_kotlin_ByteArray (*toBytes)(const char* thiz);
            } utils;
            struct {
              libshared_nativeLibs_KType* (*_type)(void);
              libshared_nativeLibs_kref_com_basic_native_Encrypt (*_instance)();
              const char* (*createSign)(libshared_nativeLibs_kref_com_basic_native_Encrypt thiz, const char* data);
              const char* (*decodeData)(libshared_nativeLibs_kref_com_basic_native_Encrypt thiz, const char* data);
              const char* (*encodeData)(libshared_nativeLibs_kref_com_basic_native_Encrypt thiz, const char* data);
            } Encrypt;
            void* (*_createSignJNI)(void* env, void* clazz, void* p0, void* p1);
            void* (*_decodeDataJNI)(void* env, void* clazz, void* p0, void* p1);
            void* (*_encodeDataJNI)(void* env, void* clazz, void* p0, void* p1);
            const char* (*createSign)(const char* source, libshared_nativeLibs_kref_com_basic_native_jvm_JvmBridge bridge);
            const char* (*decodeData)(const char* source, libshared_nativeLibs_kref_com_basic_native_jvm_JvmBridge bridge);
            const char* (*encodeData)(const char* source, libshared_nativeLibs_kref_com_basic_native_jvm_JvmBridge bridge);
            void (*checkEnv)();
          } native;
        } basic;
      } com;
      struct {
        struct {
          libshared_nativeLibs_KType* (*_type)(void);
          libshared_nativeLibs_kref_buildkonfig_BuildConfig_com_basic_nativeLibs (*_instance)();
          const char* (*get_APK_VERIFY_CODE)(libshared_nativeLibs_kref_buildkonfig_BuildConfig_com_basic_nativeLibs thiz);
        } BuildConfig_com_basic_nativeLibs;
      } buildkonfig;
    } root;
  } kotlin;
} libshared_nativeLibs_ExportedSymbols;
extern libshared_nativeLibs_ExportedSymbols* libshared_nativeLibs_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_LIBSHARED_NATIVELIBS_H */
