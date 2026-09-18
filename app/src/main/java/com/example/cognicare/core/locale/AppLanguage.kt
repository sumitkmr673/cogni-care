package com.example.cognicare.core.locale

/**
 * Languages offered at onboarding. Adding a language means adding an entry here
 * and a matching values-<tag> resource folder; nothing else is hardcoded.
 *
 * [isTranslated] says whether that resource folder actually exists yet. Entries without one
 * still appear in the pickers — the NER language list is part of the product story — but they
 * are labelled so a user is never silently handed English after choosing their own language.
 * Flip the flag when the folder lands; nothing else needs to change.
 */
enum class AppLanguage(
    val tag: String,
    val nativeName: String,
    val englishName: String,
    val isTranslated: Boolean = false
) {
    ENGLISH("en", "English", "English", isTranslated = true),
    HINDI("hi", "हिन्दी", "Hindi", isTranslated = true),
    ASSAMESE("as", "অসমীয়া", "Assamese"),
    BENGALI("bn", "বাংলা", "Bengali", isTranslated = true),
    MANIPURI("mni", "মৈতৈলোন্", "Manipuri"),
    NEPALI("ne", "नेपाली", "Nepali"),
    KHASI("kha", "Ka Ktien Khasi", "Khasi"),
    MIZO("lus", "Mizo ṭawng", "Mizo");

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }
    }
}
