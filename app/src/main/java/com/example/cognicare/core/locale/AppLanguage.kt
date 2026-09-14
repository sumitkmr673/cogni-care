package com.example.cognicare.core.locale

/**
 * Languages offered at onboarding. Adding a language means adding an entry here
 * and a matching values-<tag> resource folder; nothing else is hardcoded.
 */
enum class AppLanguage(val tag: String, val nativeName: String, val englishName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "हिन्दी", "Hindi"),
    ASSAMESE("as", "অসমীয়া", "Assamese"),
    BENGALI("bn", "বাংলা", "Bengali"),
    MANIPURI("mni", "মৈতৈলোন্", "Manipuri"),
    NEPALI("ne", "नेपाली", "Nepali"),
    KHASI("kha", "Ka Ktien Khasi", "Khasi"),
    MIZO("lus", "Mizo ṭawng", "Mizo");

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }
    }
}
