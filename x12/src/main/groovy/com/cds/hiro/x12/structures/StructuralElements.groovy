package com.cds.hiro.x12.structures

abstract class Element {
  protected static <T> T valueOf(String input, Class<T> clazz) {
    if (Enum.isAssignableFrom(clazz)) {
      clazz.byCode(input)
    } else if (clazz == String) {
      input as T
    } else if (clazz == Integer) {
      Integer.parseInt(input) as T
    } else if (clazz == Double) {
      Double.parseDouble(input) as T
    } else {
      throw new Exception("Unhandled type - ${clazz}")
    }
  }
}

abstract class Composite extends Element {
  abstract void parse(List<String> input)

}

abstract class BlockElement extends Element {}

abstract class Message extends BlockElement {
  abstract void parse(List<List<List<List<String>>>> input)
}

abstract class Loop extends BlockElement {
  abstract void parse(List<List<List<List<String>>>> input)
}

abstract class Segment extends BlockElement {
  abstract void parse(List<List<List<String>>> input)

  protected static <T> T valueOf(List<List<String>> strings, Class<T> clazz) {
    def list = listOf(strings, clazz)
    list.size() > 0 ? list.first() : null
  }

  protected static <T> List<T> listOf(List<List<String>> strings, Class<T> clazz) {
    if (Composite.isAssignableFrom(clazz)) {
      strings.collect { array ->
        clazz.newInstance().
            with { instance ->
              instance.parse(array)
              instance
            }
      }
    } else {
      strings.collect {valueOf(it[0], clazz)}
    }
  }
}
