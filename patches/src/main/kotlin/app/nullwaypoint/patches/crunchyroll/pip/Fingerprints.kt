package app.nullwaypoint.patches.crunchyroll.pip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object PictureInPictureProviderFingerprint : Fingerprint(
    filters = listOf(
        string("picture_in_picture"),
    ),
)

internal object PictureInPictureAutoEnterFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(
        string("SecurityException registering pipActionReceiver: "),
        methodCall(
            definingClass = "Landroid/app/PictureInPictureParams\$Builder;",
            name = "setAutoEnterEnabled",
        ),
    ),
)

internal object PictureInPictureSkipForwardFingerprint : Fingerprint(
    definingClass =
        "Lcom/crunchyroll/player/presentation/playerview/PlayerViewLayout;",
    name = "L",
    returnType = "V",
    parameters = emptyList(),
)

internal object SkipSegmentButtonFingerprint : Fingerprint(
    definingClass = "Lkz/e;",
    name = "a",
    returnType = "V",
    parameters = listOf(
        "Lkz/f;",
        "Landroidx/compose/ui/d;",
        "Lz0/l;",
        "I",
        "I",
    ),
)

internal object WatchScreenUserLeaveHintFingerprint : Fingerprint(
    definingClass =
        "Lcom/crunchyroll/watchscreen/screen/WatchScreenActivity;",
    name = "onUserLeaveHint",
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Landroid/view/View;",
            name = "post",
        ),
    ),
)
