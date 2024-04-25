package hexdebug.utils

import hexdebug.HexDebugProperties

open class HexDebugPropertiesExtension(project: Project) : HexDebugProperties(project)

extensions.create<HexDebugPropertiesExtension>("hexdebugProperties")
