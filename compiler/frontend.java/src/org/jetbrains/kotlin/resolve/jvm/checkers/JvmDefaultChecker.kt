/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JvmDefaultChecker(val jvmTarget: JvmTarget) : DeclarationChecker {

    companion object {
        val JVM_DEFAULT_FQ_NAME = FqName("kotlin.jvm.JvmDefault")
    }

    val enableDefaultFlag = false

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {

        if (!checkJvmDefaultFlag(descriptor)) {
            context.trace.report(ErrorsJvm.JVM_DEFAULT_THROUGH_INHERITANCE.on(declaration))
        }

        descriptor.annotations.findAnnotation(JVM_DEFAULT_FQ_NAME)?.let { annotationDescriptor ->
            val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotationDescriptor) ?: declaration
            if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_NOT_IN_INTERFACE.on(reportOn))
            } else if (jvmTarget == JvmTarget.JVM_1_6) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_JVM6_TARGET.on(reportOn))
            } else if (!enableDefaultFlag) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_DECLARATION.on(declaration))
            }
            return@check
        }

        if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) return
        val memberDescriptor = descriptor as? CallableMemberDescriptor ?: return
        if (descriptor is PropertyAccessorDescriptor) return

        if (memberDescriptor.overriddenDescriptors.any { it.annotations.hasAnnotation(JVM_DEFAULT_FQ_NAME) }) {
            context.trace.report(ErrorsJvm.JVM_DEFAULT_REQUIRED_FOR_OVERRIDE.on(declaration))
        }
    }

    fun checkJvmDefaultFlag(descriptor: DeclarationDescriptor): Boolean {
        if (enableDefaultFlag) return true

        if (descriptor !is ClassDescriptor) return true

        if (!DescriptorUtils.isInterface(descriptor) &&
            !DescriptorUtils.isAnnotationClass(descriptor)
        ) {
            return descriptor.getSuperInterfaces().all {
                checkJvmDefaultFlag(it)
            }
        }


        return descriptor.unsubstitutedMemberScope.getContributedDescriptors().filterIsInstance<CallableMemberDescriptor>().all {
            !it.hasJvmDefaultAnnotation()
        }
    }
}