package gay.`object`.hexdebug.adapter

import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.jsonrpc.Launcher

interface IHexDebugLauncher : Launcher<IDebugProtocolClient> {
    fun handleMessage(content: String)
}
