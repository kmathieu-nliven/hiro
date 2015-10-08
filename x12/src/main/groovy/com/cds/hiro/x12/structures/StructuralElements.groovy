package com.cds.hiro.x12.structures

abstract class Element {}
abstract class Composite extends Element {

}
abstract class BlockElement extends Element {}

abstract class Message extends BlockElement {
  abstract void parse(List<List<List<String[]>>> input)
}
abstract class Loop extends BlockElement {
  abstract void parse(List<List<List<String[]>>> input)
}
abstract class Segment extends BlockElement {
  abstract void parse(List<List<String[]>> input)

  protected static <T> T valueOf(List<String[]> strings, Class<T> clazz) {
    if (Enum.isAssignableFrom(clazz)) {
      clazz.byCode(strings[0][0])
    } else if (clazz == String) {
      strings[0][0]
    } else {
      throw new Exception("Unhandled type")
    }
  }

  protected static <T> List<T> listOf(List<String[]> strings, Class<T> clazz) {
    if (Enum.isAssignableFrom(clazz)) {
      strings.collect { clazz.byCode(it[0]) }
    } else if (clazz == String) {
      strings.collect {it[0]}
    } else {
      throw new Exception("Unhandled type")
    }
  }
}
