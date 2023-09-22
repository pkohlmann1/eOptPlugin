package com.github.pkohlmann1.eOptPlugin.inspections.refactoring

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.annotations.NotNull


class EncapsulateFieldInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitField(field: PsiField) {
                super.visitField(field)

                // Check if the field is accessed directly
                if (isFieldAccessedDirectly(field)) {
                    // Report the problem
                    val fix: LocalQuickFix = createEncapsulateFieldFix(field)
                    holder.registerProblem(field, "Field can be encapsulated", ProblemHighlightType.WARNING, fix)
                }
            }
        }
    }

    private fun isFieldAccessedDirectly(field: PsiField): Boolean {
        val containingClass = field.containingClass ?: return false

        // Check if the field is accessed directly in any methods or constructors
//        for (method in containingClass.methods) {
//            if (PsiUtil.isAccessible(field, method, null) && isFieldAccessedInMethod(field, method)) {
//                return true
//            }
//        }

        // Check if the field is accessed directly in any other classes
        val project = field.project
        val scope = GlobalSearchScope.projectScope(project)
        val fieldReferences = ReferencesSearch.search(field, scope).findAll()

        for (reference in fieldReferences) {
            val element = reference.element
            val containingClassOfReference = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
            if (containingClassOfReference != containingClass && isFieldAccessedInClass(field, containingClassOfReference)) {
                return true
            }
        }
        return false
    }

    private fun isFieldAccessedInClass(field: PsiField, containingClass: PsiClass?): Boolean {
        containingClass ?: return false

        // Check if the field is accessed directly in the class
        val references = PsiTreeUtil.findChildrenOfType(containingClass, PsiReferenceExpression::class.java)
        for (reference in references) {
            if (reference.resolve() === field) {
                return true
            }
        }

        return false
    }

    private fun isFieldAccessedInMethod(field: PsiField, method: PsiMethod): Boolean {
        val body = method.body ?: return false

        // Check if the field is accessed directly in the method's body
        return PsiTreeUtil.findChildrenOfType(body, PsiReferenceExpression::class.java).stream()
            .anyMatch { expression: PsiReferenceExpression -> expression.resolve() === field }
    }

    private fun createEncapsulateFieldFix(field: PsiField): LocalQuickFix {
        return object : LocalQuickFix {
            @NotNull
            override fun getName(): String {
                return "Encapsulate field"
            }

            @NotNull
            override fun getFamilyName(): String {
                return "Encapsulate field"
            }

            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                val elementFactory = JavaPsiFacade.getElementFactory(project)
                val containingClass = field.containingClass
                if (containingClass != null) {
                    val getterMethod = elementFactory.createMethod(getGetterMethodName(field), field.type)
                    getterMethod.body!!.add(
                        elementFactory.createStatementFromText(
                            "return " + field.name + ";", containingClass
                        )
                    )
                    containingClass.add(getterMethod)
                    val setterMethod = elementFactory.createMethod(getSetterMethodName(field), PsiType.VOID)
                    val parameter = elementFactory.createParameter(field.name, field.type)
                    setterMethod.parameterList.add(parameter)
                    setterMethod.body!!.add(
                        elementFactory.createStatementFromText(
                            "this." + field.name + " = " + parameter.name + ";", containingClass
                        )
                    )
                    containingClass.add(setterMethod)
                    field.modifierList?.setModifierProperty(PsiModifier.PUBLIC, false);
                    field.modifierList?.setModifierProperty(PsiModifier.PRIVATE, true);

                    // Check if the field is accessed directly in any other classes
                    val project = field.project
                    val scope = GlobalSearchScope.projectScope(project)
                    val fieldReferences = ReferencesSearch.search(field, scope).findAll()

                    for (reference in fieldReferences) {
                        val element = reference.element
                        val containingClassOfReference = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                        if (containingClassOfReference != containingClass && isFieldAccessedInClass(field, containingClassOfReference)) {

                            // Replace the direct field access with a method invocation
                            val factory = JavaPsiFacade.getElementFactory(project)
                            val fieldAccess = element.text.replace(field.name, "");
                            val methodName = if (PsiUtil.isAccessedForWriting(element as PsiExpression)) getSetterMethodName(field) else getGetterMethodName(field)
                            if (methodName.contains("set")) {
                                val parent = element.parent
                                val rightAssignment = parent.text
                                val lastPart: String? = rightAssignment?.substringAfterLast("=")?.trim()
                                val methodInvocation = factory.createExpressionFromText("$fieldAccess$methodName($lastPart)", element)
                                parent.replace(methodInvocation)
                            } else if (methodName.contains("get")){
                                val methodInvocation = factory.createExpressionFromText("$fieldAccess$methodName()", element)
                                element.replace(methodInvocation)
                            }
                        }
                    }

                }
            }
        }
    }

    private fun getGetterMethodName(field: PsiField): String {
        val fieldName: String = field.getName();
        val capitalizedFieldName: String = Character.toUpperCase(fieldName[0]) + fieldName.substring(1);
        return "get$capitalizedFieldName";
    }

    private fun getSetterMethodName(field: PsiField): String {
        val fieldName: String = field.getName();
        val capitalizedFieldName: String = Character.toUpperCase(fieldName[0]) + fieldName.substring(1);
        return "set$capitalizedFieldName";
    }
}

