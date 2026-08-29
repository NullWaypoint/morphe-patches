package app.nullwaypoint.patches.crunchyroll.removecontinue

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object ContinueWatchingOverflowMenuFingerprint : Fingerprint(
    returnType = "Ljava/util/List;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        string("data"),
        methodCall(
            parameters = emptyList(),
            returnType = "Z",
            opcode = Opcode.INVOKE_INTERFACE,
        ),
    ),
)

internal object CrunchyrollApplicationOnCreateFingerprint : Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/app/Application;",
            name = "onCreate",
        ),
        string("APP onCreate"),
    ),
)

internal object ContinueWatchingFeedMapperFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Lcom/ellation/crunchyroll/model/Panel;",
            name = "getContinueWatchingImages",
        ),
        methodCall(
            definingClass = "Lcom/ellation/crunchyroll/model/Panel;",
            name = "getId",
        ),
        methodCall(
            definingClass = "Lcom/ellation/crunchyroll/api/etp/model/Image;",
            name = "getUrl",
        ),
    ),
)