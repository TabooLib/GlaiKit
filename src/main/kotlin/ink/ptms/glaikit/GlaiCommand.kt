package ink.ptms.glaikit

import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.expansion.createHelper

@CommandHeader(name = "glaikit", permission = "*", aliases = ["kit"])
object GlaiCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val eval = subCommand {
        // 运行
    }

    @CommandBody
    val release = subCommand {
        // 释放
    }

    @CommandBody
    val compile = subCommand {
        // 编译
    }
}