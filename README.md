# 🔧 NullWaypoint Patches

Custom Android patches for the Morphe patching framework.

## ❓ About

A small collection of patches for apps I use, mainly to enable or restore useful features.

### How to use these patches

Add NullWaypoint Patches to Morphe:

https://morphe.software/add-source?github=NullWaypoint/morphe-patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.1.0-dev.1](https://github.com/NullWaypoint/morphe-patches/releases/tag/v1.1.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;2 patches total
<details open>
<summary>📦 Crunchyroll&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**🎯 Supported versions:**

| 3.117.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Enable Picture-in-Picture](#enable-picture-in-picture) | Enables Crunchyroll's native Picture-in-Picture and adds a PiP button to the player. |  |
| [Remove from Continue Watching](#remove-from-continue-watching) | Adds an option to remove titles from Continue Watching. |  |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The generated `.mpp` patch bundle can be found in `patches/build/libs/patches-*.mpp`
- Load the `.mpp` file in [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) like any other patch bundle

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

NullWaypoint Patches is licensed under the [GNU General Public License v3.0](LICENSE).