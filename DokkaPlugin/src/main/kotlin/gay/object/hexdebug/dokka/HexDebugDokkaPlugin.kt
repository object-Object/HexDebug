package gay.`object`.hexdebug.dokka

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

// https://kotlin.github.io/dokka/2.0.0/developer_guide/plugin-development/sample-plugin-tutorial/

class HexDebugDokkaPlugin : DokkaPlugin() {
    @Suppress("unused")
    val filterExtension by extending {
        plugin<DokkaBase>().preMergeDocumentableTransformer providing ::HideInternalApiTransformer
    }

    @DokkaPluginApiPreview
    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement
}

class HideInternalApiTransformer(context: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(context)
{
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        if (d !is WithExtraProperties<*>) return false
        return d.extra
            .allOfType<Annotations>()
            .flatMap { it.directAnnotations.values.flatten() }
            .any { isInternalAnnotation(it.dri) }
    }

    private fun isInternalAnnotation(dri: DRI) =
        dri.packageName == "org.jetbrains.annotations" && dri.classNames == "ApiStatus.Internal"
}
