package com.vaultsec.vaultsec.util.exclusion

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

/*
* @Exclude annotation to exclude certain variables from serialization while sending them over retrofit
* */

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Exclude {
}

class AnnotationExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipField(f: FieldAttributes?): Boolean {
        return f?.getAnnotation(Exclude::class.java) != null
    }

    override fun shouldSkipClass(clazz: Class<*>?): Boolean {
        return false
    }
}
