package org.marid.moan

class MoanCreationException(name: String, cause: Throwable) : RuntimeException(name, cause)
class MoanDestructionException(name: String) : RuntimeException(name)
class ScopeDestructionException(name: String) : RuntimeException(name)
class DuplicatedMoanException(name: String): RuntimeException(name)