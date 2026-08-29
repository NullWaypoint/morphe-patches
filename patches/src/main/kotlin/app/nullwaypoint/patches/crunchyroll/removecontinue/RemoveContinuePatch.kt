package app.nullwaypoint.patches.crunchyroll.removecontinue

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.nullwaypoint.patches.shared.Constants.COMPATIBILITY_CRUNCHYROLL
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val crunchyrollRemoveContinuePatch = bytecodePatch(
    name = "Remove from Continue Watching",
    description = "Adds an option to remove titles from Continue Watching.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_CRUNCHYROLL)

    extendWith("extensions/extension.mpe")

    execute {
        with(CrunchyrollApplicationOnCreateFingerprint) {
            val superOnCreate = instructionMatches.first()

            method.addInstructions(
                superOnCreate.index + 1,
                """
                    invoke-static {p0}, Lapp/nullwaypoint/extension/crunchyroll/RemoveContinue;->initialize(Landroid/content/Context;)V
                """,
            )
        }

        with(ContinueWatchingOverflowMenuFingerprint) {
            method.addInstructions(
                0,
                """
                    invoke-static {p1}, Lapp/nullwaypoint/extension/crunchyroll/RemoveContinue;->captureData(Ljava/lang/Object;)V
                """,
            )

            val implementation = method.implementation
                ?: throw PatchException("Continue Watching menu provider has no implementation")

            val returnIndex = implementation.instructions.indexOfLast {
                it.opcode == Opcode.RETURN_OBJECT
            }

            if (returnIndex < 0) {
                throw PatchException("Continue Watching menu return instruction not found")
            }

            val returnRegister =
                (implementation.instructions.elementAt(returnIndex) as? OneRegisterInstruction)
                    ?.registerA
                    ?: throw PatchException("Continue Watching menu return register not found")

            method.addInstructions(
                returnIndex,
                """
                    invoke-static {v$returnRegister, p0}, Lapp/nullwaypoint/extension/crunchyroll/RemoveContinue;->addRemoveContinue(Ljava/util/List;Ljava/lang/Object;)Ljava/util/List;
                    move-result-object v$returnRegister
                """,
            )
        }

        with(ContinueWatchingFeedMapperFingerprint) {
            val mapperClass = mutableClassDefBy(method.definingClass)

            val mappingMethods = mapperClass.methods.filter { candidate ->
                val instructions =
                    candidate.implementation?.instructions
                        ?: return@filter false

                val references = instructions.mapNotNull { instruction ->
                    (instruction as? ReferenceInstruction)
                        ?.reference as? MethodReference
                }

                val hasPanelId = references.any { reference ->
                    reference.definingClass ==
                        "Lcom/ellation/crunchyroll/model/Panel;" &&
                        reference.name == "getId"
                }

                val hasContinueWatchingImages = references.any { reference ->
                    reference.definingClass ==
                        "Lcom/ellation/crunchyroll/model/Panel;" &&
                        reference.name == "getContinueWatchingImages"
                }

                hasPanelId && hasContinueWatchingImages
            }

            if (mappingMethods.isEmpty()) {
                throw PatchException("Continue Watching feed mapping methods not found")
            }

            mappingMethods.forEach { mappingMethod ->
                mappingMethod.addInstructions(
                    0,
                    """
                        invoke-static/range {p0 .. p0}, Lapp/nullwaypoint/extension/crunchyroll/RemoveContinue;->filterFeed(Ljava/lang/Object;)V
                    """,
                )
            }
        }
    }
}