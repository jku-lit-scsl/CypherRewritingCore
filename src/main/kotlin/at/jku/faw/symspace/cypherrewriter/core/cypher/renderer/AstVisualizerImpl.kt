/*
 * Cypher Rewriting System
 * Copyright (C) 2023 Daniel Hofer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.jku.faw.symspace.cypherrewriter.core.cypher.renderer

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import org.springframework.stereotype.Component

@Component
class AstVisualizerImpl : AstVisualizer {

    private val indentation: String = "    "
    override fun render(astNode: AstNode): String {
        return render(astNode, 0)
    }

    private fun render(astNode: AstNode, level: Int): String {
        return when (astNode) {
            is AstInternalNode -> renderNode(astNode, level)
            is AstLeafValue -> renderValue(astNode, level)
            is AstLeafNoValue -> renderTerminal(astNode, level)
        }
    }

    private fun renderTerminal(astLeafNoValue: AstLeafNoValue, level: Int): String {
        return getIndentation(level) + astLeafNoValue.type.name + "\n"
    }

    private fun renderValue(astValue: AstLeafValue, level: Int): String {
        return getIndentation(level) + astValue.type.name + " = " + astValue.value + "\n"
    }

    private fun renderNode(astNode: AstInternalNode, level: Int): String {
        return getIndentation(level) + astNode.type.name + " {\n" +
                astNode.elements.joinToString(separator = "") { render(it, level + 1) } +
                getIndentation(level) + "}\n"
    }

    private fun getIndentation(level: Int): String {
        return indentation.repeat(level)
    }


}
