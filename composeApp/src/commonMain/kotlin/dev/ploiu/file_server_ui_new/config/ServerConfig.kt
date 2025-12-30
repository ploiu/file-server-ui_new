package dev.ploiu.file_server_ui_new.config

import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors

class ServerConfig(
    val baseUrl: String,
    val compatibleVersion: String,
) {
    fun generateCompatibleVersionPattern(): Pattern {
        // our regex builder will only work if the whole version follows the right property
        val versionMatcher = Pattern.compile("^\\d+(\\.(\\d+|x)){2}$")
        if (!versionMatcher.matcher(compatibleVersion).find()) {
            throw RuntimeException("Bad compatible version $compatibleVersion. Version must follow the format #.(#|x).(#|x) format. e.g. 1.2.x")
        }
        val versionRegex = Arrays.stream(
                compatibleVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(),
            ).map { part: String? -> part!!.replace("x", "\\d+") }.collect(Collectors.joining("."))
        return Pattern.compile(versionRegex)
    }
}
