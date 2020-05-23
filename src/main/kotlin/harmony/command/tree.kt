package harmony.command

import harmony.util.InvokeHandle

/**
 * A node used to represent commands as a tree of arguments.
 */
class Node(val type: Class<*> = Void::class.java,
           var obj: InvokeHandle? = null,
           private val _children: MutableMap<Class<*>, Node> = mutableMapOf(),
           private var parent: Node? = null) {

    val children: Map<Class<*>, Node>
        get() = _children

    val isTerminal: Boolean
        get() = children.isEmpty()

    val isRoot: Boolean
        get() = type == Void::class.java

    fun addChild(node: Node) {
        node.parent = this
        _children[node.type] = node
    }
}

typealias Tree = Node

fun generateCandidates(tree: Tree, maxParamCount: Int): List<InvokeHandle> { // Do a BFS over the tree up to the max level
    //TODO prioritize non-string types
    var currLevel = 0

    var level = listOf(tree)
    var nextLevel = mutableListOf<Node>()
    val handles = mutableListOf<InvokeHandle>()
    while (currLevel <= maxParamCount && level.isNotEmpty()) {
        for (node in level) {
            if (node.obj != null) {
                handles.add(node.obj!!)
            }

            if (!node.isTerminal) {
                node.children.values.forEach { nextLevel.add(it) }
            }
        }
        level = nextLevel
        nextLevel = mutableListOf()

        currLevel += 1
    }

    return handles.reversed()
}
